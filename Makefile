.PHONY: infra run stop clean
infra:
	docker compose up -d
run:
	mvn spring-boot:run
stop:
	docker compose down
clean:
	docker compose down -v
