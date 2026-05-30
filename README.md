# My Market App

Учебный Maven-мультипроект витрины интернет-магазина для 7 спринта.

Проект состоит из основного приложения витрины, отдельного RESTful-сервиса платежей и OpenAPI-контракта между ними. Основное приложение использует Spring WebFlux, Thymeleaf, R2DBC H2 и Redis-кеш товаров. Сервис платежей работает на Spring WebFlux и embedded Netty.

## Требования

- Java 21
- Docker Desktop
- PowerShell для команд в Windows

## Структура проекта

```text
my-market-app/
  pom.xml
  api/payment-api.yaml
  market-app/
  payment-service/
  docker-compose.yml
```

- `market-app` - веб-приложение витрины на порту `8080`.
- `payment-service` - сервис платежей на порту `8081`.
- `api/payment-api.yaml` - OpenAPI-спецификация платежей.
- `docker-compose.yml` - локальный запуск `market-app`, `payment-service` и Redis.

## Конфигурация

Основные настройки `market-app`:

- `spring.data.redis.host` - хост Redis, по умолчанию `localhost`.
- `spring.data.redis.port` - порт Redis, по умолчанию `6379`.
- `app.items.cache.ttl` - TTL кеша товаров, по умолчанию `2m`.
- `app.payment-service.base-url` - URL сервиса платежей, по умолчанию `http://localhost:8081`.

Основные настройки `payment-service`:

- `server.port` - порт сервиса, по умолчанию `8081`.
- `payment.initial-balance` - начальный баланс, по умолчанию `10000`.

В Docker Compose для `market-app` заданы переменные:

```text
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
APP_PAYMENT_SERVICE_BASE_URL=http://payment-service:8081
```

## Сборка и тесты

Запустить все тесты мультипроекта:

```powershell
.\mvnw.cmd test
```

Запустить тесты основного приложения:

```powershell
.\mvnw.cmd -pl market-app test
```

Запустить тесты сервиса платежей:

```powershell
.\mvnw.cmd -pl payment-service test
```

Собрать executable JAR для всех модулей:

```powershell
.\mvnw.cmd clean package
```

JAR-файлы создаются в:

```text
market-app/target/market-app-0.0.1-SNAPSHOT.jar
payment-service/target/payment-service-0.0.1-SNAPSHOT.jar
```

## Локальный запуск без Docker Compose

Сначала запустите Redis на `localhost:6379`.

Запустить сервис платежей:

```powershell
.\mvnw.cmd -pl payment-service spring-boot:run
```

Запустить витрину во втором терминале:

```powershell
.\mvnw.cmd -pl market-app spring-boot:run
```

После запуска:

- витрина доступна на `http://localhost:8080`;
- сервис платежей доступен на `http://localhost:8081`.

## Docker

Собрать образ сервиса платежей:

```powershell
docker build -t payment-service:sprint-7 -f .\payment-service\Dockerfile .
```

Собрать образ витрины:

```powershell
docker build -t market-app:sprint-7 -f .\market-app\Dockerfile .
```

## Docker Compose

Собрать и запустить весь стек:

```powershell
docker compose up --build
```

Compose поднимает:

- `market-app` на `http://localhost:8080`;
- `payment-service` на `http://localhost:8081`;
- Redis на `localhost:6379`.

Остановить стек:

```powershell
docker compose down
```

## OpenAPI и платежи

Контракт платежей описан в `api/payment-api.yaml`.

По спецификации генерируются:

- reactive WebClient-клиент для `market-app`;
- reactive Spring server interfaces/models для `payment-service`.

Основные endpoints сервиса платежей:

- `GET /payments/balance` - получить текущий баланс.
- `POST /payments/pay` - списать сумму заказа.

Обмен между сервисами выполняется в JSON.

## Redis-кеш товаров

`market-app` использует Redis как кеш товаров:

- кешируется список товаров;
- кешируется карточка товара;
- при cache miss данные загружаются из R2DBC H2 и сохраняются в Redis;
- TTL кеша задан настройкой `app.items.cache.ttl=2m`.

## Использование витрины

- Откройте `/` или `/items`, чтобы увидеть витрину товаров.
- Используйте поиск, сортировку и пагинацию на витрине.
- Откройте `/items/{id}`, чтобы посмотреть карточку товара.
- Добавляйте и уменьшайте количество товаров кнопками `+` и `-`.
- Откройте `/cart/items`, чтобы проверить корзину и итоговую сумму.
- Нажмите `Купить`, чтобы отправить `POST /buy`.
- Откройте `/orders`, чтобы увидеть список заказов.
- Откройте `/orders/{id}`, чтобы увидеть страницу одного заказа.

Покупка создаёт заказ только после успешного платежа. Если баланс меньше суммы корзины или сервис платежей недоступен, кнопка покупки недоступна и пользователь видит сообщение. Если платёж не прошёл, заказ не создаётся.

## Основные маршруты market-app

- `GET /` - витрина товаров.
- `GET /items` - витрина товаров с поиском, сортировкой и пагинацией.
- `POST /items` - изменение количества товара с витрины.
- `GET /items/{id}` - страница товара.
- `POST /items/{id}` - изменение количества товара со страницы товара.
- `GET /cart/items` - корзина.
- `POST /cart/items` - изменение количества товара в корзине.
- `POST /buy` - оформление заказа с оплатой.
- `GET /orders` - список заказов.
- `GET /orders/{id}` - страница заказа.

## Данные

Схема БД создаётся из `market-app/src/main/resources/schema.sql`.
Начальные товары загружаются из `market-app/src/main/resources/data.sql`.
