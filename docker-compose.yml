services:
  db:
    image: mysql:8.4
    container_name: herlink-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: heritageResourcePlatform
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - mysql_data:/var/lib/mysql
      - ./backend/sql/init_database.sql:/docker-entrypoint-initdb.d/01-init_database.sql:ro
    healthcheck:
      test: ['CMD-SHELL', 'mysqladmin ping -h localhost -u root -p$${MYSQL_ROOT_PASSWORD} || exit 1']
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - herlink-network

  app:
    image: ${APP_IMAGE:-herlink-app:local}
    build:
      context: .
      dockerfile: Dockerfile
    container_name: herlink-app
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://db:3306/heritageResourcePlatform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8
      DB_USERNAME: root
      DB_PASSWORD: ${MYSQL_ROOT_PASSWORD}

      DEMO_DATA_ENABLED: 'false'

      MAIL_HOST: ${MAIL_HOST}
      MAIL_PORT: ${MAIL_PORT}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      MAIL_FROM: ${MAIL_FROM}
      MAIL_SMTP_AUTH: ${MAIL_SMTP_AUTH}
      MAIL_SMTP_STARTTLS_ENABLE: ${MAIL_SMTP_STARTTLS_ENABLE}

      HERLINK_UPLOAD_DIR: /app/uploads
      HERLINK_FRONTEND_DIR: /app/frontend

      JAVA_OPTS: ${JAVA_OPTS}
    volumes:
      - app_uploads:/app/uploads
    expose:
      - '8080'
    networks:
      - herlink-network

  nginx:
    image: nginx:stable-alpine
    container_name: herlink-nginx
    restart: unless-stopped
    depends_on:
      - app
    ports:
      - '80:80'
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf:ro
    networks:
      - herlink-network

volumes:
  mysql_data:
  app_uploads:

networks:
  herlink-network:
    driver: bridge
