# My Market App

Учебный Maven-мультипроект витрины интернет-магазина для спринта 8.

Проект состоит из WebFlux-витрины, отдельного сервиса платежей, Redis-кеша товаров и Keycloak для OAuth2/OIDC. Витрина использует browser login через Keycloak, хранит локальный профиль пользователя по Keycloak username и вызывает сервис платежей с OAuth2 Bearer token.

## Требования

- Java 21
- Docker Desktop
- PowerShell для команд в Windows

## Структура проекта

```text
my-market-app/
  pom.xml
  api/payment-api.yaml
  keycloak/realm-export.json
  market-app/
  payment-service/
  docker-compose.yml
```

- `market-app` - веб-приложение витрины на `http://localhost:8080`.
- `payment-service` - сервис платежей на `http://localhost:8081`.
- `keycloak` - OAuth2/OIDC сервер на `http://localhost:8082`.
- `api/payment-api.yaml` - OpenAPI-спецификация платежей.
- `docker-compose.yml` - локальный запуск `market-app`, `payment-service`, Redis и Keycloak.

## Конфигурация

Основные настройки `market-app`:

- `spring.data.redis.host` - хост Redis, по умолчанию `localhost`.
- `spring.data.redis.port` - порт Redis, по умолчанию `6379`.
- `app.items.cache.ttl` - TTL кеша товаров, по умолчанию `2m`.
- `app.payment-service.base-url` - URL сервиса платежей, по умолчанию `http://localhost:8081`.
- `PAYMENT_SERVICE_CLIENT_ID` - OAuth2 client id для вызова платежей, по умолчанию `market-app`.
- `PAYMENT_SERVICE_CLIENT_SECRET` - dev-секрет клиента платежей.
- `KEYCLOAK_LOGIN_CLIENT_ID` - OAuth2 client id для browser login, по умолчанию `market-app-login`.
- `KEYCLOAK_LOGIN_CLIENT_SECRET` - dev-секрет клиента browser login.
- `KEYCLOAK_AUTHORIZATION_URI`, `KEYCLOAK_TOKEN_URI`, `KEYCLOAK_JWK_SET_URI`, `KEYCLOAK_USER_INFO_URI` - endpoints Keycloak.

Основные настройки `payment-service`:

- `server.port` - порт сервиса, по умолчанию `8081`.
- `payment.initial-balance` - начальный баланс каждого пользователя, по умолчанию `10000`.
- `KEYCLOAK_ISSUER_URI` - issuer realm Keycloak для проверки JWT.

В `docker-compose.yml` используются тестовые dev-секреты из `keycloak/realm-export.json`.
## Сборка и тесты

Запустить все тесты мультипроекта:

```powershell
.\mvnw.cmd clean test
```

Запустить тесты основного приложения:

```powershell
.\mvnw.cmd -pl market-app test
```

Запустить тесты сервиса платежей:

```powershell
.\mvnw.cmd -pl payment-service test
```

Проверить Docker Compose конфигурацию:

```powershell
docker compose config
```

Собрать executable JAR для всех модулей:

```powershell
.\mvnw.cmd clean package
```

## Docker Compose

Если уже поднята старая версия окружения, остановите её и пересоберите образы:

```powershell
docker compose down --remove-orphans
docker compose build --no-cache
docker compose up -d
docker compose ps
```

Compose поднимает:

- `market-app` на `http://localhost:8080`;
- `payment-service` на `http://localhost:8081`;
- Keycloak на `http://localhost:8082`;
- Redis на `localhost:6379`.

Остановить стек:

```powershell
docker compose down
```

## Keycloak

Realm импортируется из `keycloak/realm-export.json`.

Keycloak admin UI:

- URL: `http://localhost:8082`
- login: `admin`
- password: `admin`

Realm: `my-market`.

Тестовые пользователи витрины:

- `user` / `password`
- `buyer` / `password`

OAuth2 clients:

- `market-app-login` - browser login для пользователей витрины;
- `market-app` - client credentials flow для вызовов `payment-service`.

## OAuth2 и платежи

`market-app` использует два OAuth2-сценария:

- browser login через `oauth2Login()` и клиент `market-app-login`;
- client credentials flow через клиент `market-app` для запросов в `payment-service`.

`payment-service` работает как OAuth2 Resource Server:

- `GET /payments/balance` требует валидный Bearer token;
- `POST /payments/pay` требует валидный Bearer token.

Так как client credentials token авторизует приложение, а не покупателя, `market-app` дополнительно передаёт текущего пользователя в заголовке `X-User-Name`. `payment-service` принимает этот заголовок только вместе с валидным Bearer token и ведёт отдельный баланс для каждого username.

Проверить, что `payment-service` закрыт без токена:

```powershell
curl.exe -i http://localhost:8081/payments/balance
```

Ожидаемый результат: `401 Unauthorized`.

## Использование витрины

1. Откройте `http://localhost:8080`.
2. Без логина доступны витрина и карточки товаров.
3. Попытка открыть `http://localhost:8080/cart/items` перенаправит на Keycloak login.
4. Войдите как `user/password`.
5. Добавьте товар в корзину и оформите заказ.
6. Выйдите и войдите как `buyer/password`.
7. Убедитесь, что корзина, заказы и платежный баланс отличаются от пользователя `user`.

Покупка создаёт заказ только после успешного платежа. Если баланс меньше суммы корзины или сервис платежей недоступен, кнопка покупки недоступна и пользователь видит соответсвующее сообщение. Если платёж отклонён, заказ не создаётся.

## Основные маршруты market-app

Публичные:

- `GET /` - витрина товаров.
- `GET /items` - витрина товаров с поиском, сортировкой и пагинацией.
- `GET /items/{id}` - страница товара.

Только для авторизованного пользователя:

- `POST /items` - изменение количества товара с витрины.
- `POST /items/{id}` - изменение количества товара со страницы товара.
- `GET /cart/items` - корзина.
- `POST /cart/items` - изменение количества товара в корзине.
- `POST /buy` - оформление заказа с оплатой.
- `GET /orders` - список заказов текущего пользователя.
- `GET /orders/{id}` - страница заказа текущего пользователя.
- `POST /logout` - выход.

## Данные

Схема БД создаётся из `market-app/src/main/resources/schema.sql`.
Начальные товары и локальные пользователи загружаются из `market-app/src/main/resources/data.sql`.

Локальная таблица `users` хранит профиль приложения, связанный с Keycloak username. Пароли пользователей находятся в Keycloak dev realm, вместо БД `market-app`, как было в предыдущих спринтах

## OpenAPI

Контракт платежей описан в `api/payment-api.yaml`.

По спецификации генерируются:

- reactive WebClient-клиент для `market-app`;
- reactive Spring server interfaces/models для `payment-service`.

Обмен между сервисами выполняется в JSON.

## Redis-кеш товаров

`market-app` использует Redis как кеш товаров:

- кешируется список товаров;
- кешируется карточка товара;
- при cache miss данные загружаются из R2DBC H2 и сохраняются в Redis;
- TTL кеша задан настройкой `app.items.cache.ttl=2m`.
