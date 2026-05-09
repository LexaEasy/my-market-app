# My Market App

Учебное Spring Boot приложение витрины товаров для спринта 5.

## Требования

- Java 21
- Docker Desktop, если нужен запуск в контейнере

## Локальный запуск

В PowerShell из корня проекта:

```powershell
.\mvnw.cmd spring-boot:run
```

Приложение будет доступно по адресу:

```text
http://localhost:8080
```

## Тесты

```powershell
.\mvnw.cmd test
```

## Сборка JAR

```powershell
.\mvnw.cmd clean package
```

После сборки executable JAR будет создан в каталоге `target`.

## Запуск через Docker

Собрать образ:

```powershell
docker build -t my-market-app:local .
```

Запустить контейнер:

```powershell
docker run --rm -p 8080:8080 my-market-app:local
```

Открыть приложение:

```text
http://localhost:8080
```

## Основные маршруты

- `/` - главная витрина товаров
- `/items` - список товаров с поиском, сортировкой и пагинацией
- `/items/{id}` - страница товара
- `/cart/items` - корзина
- `/buy` - оформление заказа
- `/orders` - список заказов
- `/orders/{id}` - страница заказа
