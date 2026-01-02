.PHONY: help run test build clean docker-up docker-down

help:
	@echo "Available commands:"
	@echo "  make run         - Run Spring Boot app"
	@echo "  make test        - Run tests"
	@echo "  make build       - Build JAR"
	@echo "  make docker-up   - Start PostgreSQL"
	@echo "  make docker-down - Stop PostgreSQL"
	@echo "  make clean       - Clean build artifacts"

run:
	./mvnw spring-boot:run

test:
	./mvnw test

build:
	./mvnw clean package -DskipTests

docker-up:
	docker-compose up -d

docker-down:
	docker-compose down

clean:
	./mvnw clean
