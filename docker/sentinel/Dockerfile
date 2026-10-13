# 官方 Dashboard JAR 没有在本项目发布自制公共镜像，因此从官方固定版本构建。
FROM eclipse-temurin:21-jre
ADD https://github.com/alibaba/Sentinel/releases/download/1.8.9/sentinel-dashboard-1.8.9.jar /app/sentinel-dashboard.jar
WORKDIR /app
EXPOSE 8858
