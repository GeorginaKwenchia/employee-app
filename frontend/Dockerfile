FROM nginx:alpine

RUN apk add --no-cache iputils curl bind-tools

COPY index.html /usr/share/nginx/html/index.html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
