const express = require("express");
const cors = require("cors");
const { Pool } = require("pg");

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

const pool = new Pool({
  connectionString: process.env.DATABASE_URL || "postgresql://postgres:postgres@localhost:5432/employees",
});

// Create table on startup
async function init() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS employees (
      id SERIAL PRIMARY KEY,
      name VARCHAR(100) NOT NULL,
      email VARCHAR(100) UNIQUE NOT NULL,
      role VARCHAR(100) NOT NULL,
      department VARCHAR(100) NOT NULL,
      dob VARCHAR(10),
      photo_url VARCHAR(500)
    )
  `);
  console.log("Database tables initialized");
}

// GET /api/health
app.get("/api/health", async (req, res) => {
  try {
    await pool.query("SELECT 1");
    res.json({ status: "healthy", db: "connected" });
  } catch (e) {
    res.status(503).json({ status: "unhealthy", db: "disconnected" });
  }
});

// GET /api/employees
app.get("/api/employees", async (req, res) => {
  const result = await pool.query("SELECT * FROM employees ORDER BY name");
  res.json(result.rows);
});

// GET /api/employees/:id
app.get("/api/employees/:id", async (req, res) => {
  const result = await pool.query("SELECT * FROM employees WHERE id = $1", [req.params.id]);
  if (result.rows.length === 0) return res.status(404).json({ error: "Not found" });
  res.json(result.rows[0]);
});

// POST /api/employees
app.post("/api/employees", async (req, res) => {
  const { name, email, role, department, dob, photo_url } = req.body;
  if (!name || !email || !role || !department) {
    return res.status(400).json({ error: "Missing required fields" });
  }
  try {
    const result = await pool.query(
      "INSERT INTO employees (name, email, role, department, dob, photo_url) VALUES ($1,$2,$3,$4,$5,$6) RETURNING *",
      [name, email, role, department, dob || null, photo_url || null]
    );
    res.status(201).json(result.rows[0]);
  } catch (e) {
    if (e.code === "23505") return res.status(409).json({ error: "Email already exists" });
    throw e;
  }
});

// PUT /api/employees/:id
app.put("/api/employees/:id", async (req, res) => {
  const existing = await pool.query("SELECT * FROM employees WHERE id = $1", [req.params.id]);
  if (existing.rows.length === 0) return res.status(404).json({ error: "Not found" });
  const emp = existing.rows[0];
  const { name, email, role, department, dob, photo_url } = req.body;
  const result = await pool.query(
    "UPDATE employees SET name=$1, email=$2, role=$3, department=$4, dob=$5, photo_url=$6 WHERE id=$7 RETURNING *",
    [name || emp.name, email || emp.email, role || emp.role, department || emp.department,
     dob || emp.dob, photo_url || emp.photo_url, req.params.id]
  );
  res.json(result.rows[0]);
});

// DELETE /api/employees/:id
app.delete("/api/employees/:id", async (req, res) => {
  const result = await pool.query("DELETE FROM employees WHERE id = $1 RETURNING *", [req.params.id]);
  if (result.rows.length === 0) return res.status(404).json({ error: "Not found" });
  res.json({ message: "Employee deleted" });
});

// GET /api/stats
app.get("/api/stats", async (req, res) => {
  const total = await pool.query("SELECT COUNT(*) FROM employees");
  const depts = await pool.query("SELECT department, COUNT(id) FROM employees GROUP BY department");
  const latest = await pool.query("SELECT * FROM employees ORDER BY id DESC LIMIT 1");
  const departments = {};
  depts.rows.forEach(r => { departments[r.department] = parseInt(r.count); });
  res.json({
    total_employees: parseInt(total.rows[0].count),
    departments,
    latest_hire: latest.rows[0] || null,
    version: "2.0.0",
  });
});

const PORT = process.env.PORT || 5000;

if (require.main === module) {
  init().then(() => {
    app.listen(PORT, () => console.log(`Backend running on http://localhost:${PORT}`));
  });
}

module.exports = { app, pool, init };
