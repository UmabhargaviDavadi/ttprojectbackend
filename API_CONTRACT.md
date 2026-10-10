# Mintwise API Contract

Base URL: `http://localhost:8080/api` (overridable with `VITE_API_BASE_URL`). All routes below are relative to the base URL. JSON request/response bodies use UTF-8. Authenticated routes require `Authorization: Bearer <JWT>`; register and login are public. Dates are ISO `YYYY-MM-DD`; timestamps are ISO 8601. Amounts are positive JSON numbers and transaction type is lowercase `income` or `expense`.

The mock layer in `src/api/mockData.js` returns these same success shapes. It uses local browser storage for mutable demo data. Error responses should use `{ "message": "Human-readable explanation" }`; the client shows the message when available. Any authenticated `401` clears the local session and redirects to `/login`.

## Authentication

`POST /auth/register`

Request:

```json
{ "name": "Aarav Mehta", "email": "aarav@example.com", "password": "example-password" }
```

`POST /auth/login`

Request: `{ "email": "aarav@example.com", "password": "example-password" }`

Both return:

```json
{
  "token": "jwt-token",
  "user": { "id": 1, "name": "Aarav Mehta", "email": "aarav@example.com", "currency": "INR" }
}
```

## Transactions

`GET /transactions` accepts `page` (zero-based), `size`, `category`, `type`, `from`, `to`, `search`, `minAmount`, `maxAmount`, and `sort` (for example `date,desc`, `amount,asc`, `description,asc`). `minAmount`, `maxAmount`, and `sort` are included to support the requested amount filters and sorting alongside the supplied endpoint parameters.

Response:

```json
{
  "content": [
    {
      "id": 101,
      "date": "2026-10-10",
      "description": "Groceries",
      "category": "Food",
      "type": "expense",
      "amount": 1280,
      "paymentMethod": "UPI"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 10
}
```

`POST /transactions` accepts `{ "date": "2026-10-10", "description": "Groceries", "category": "Food", "type": "expense", "amount": 1280, "paymentMethod": "UPI" }`. `paymentMethod` is optional. It returns the created transaction with its server-assigned `id`.

`PUT /transactions/{id}` accepts the same transaction fields and returns the updated transaction. `DELETE /transactions/{id}` returns `{ "success": true }`.

Expense categories: Food, Travel, Entertainment, Bills, Shopping, Health, Education, Other. Income categories: Salary, Allowance, Freelance, Investment, Other.

## Budgets

`GET /budgets?month=10&year=2026` returns an array:

```json
[{ "id": 1, "category": "Overall", "limit": 30000, "month": 10, "year": 2026 }]
```

Category values are `Overall` or an expense category. `POST /budgets` accepts `{ "category": "Food", "limit": 8000, "month": 10, "year": 2026 }` and returns the created budget. `PUT /budgets/{id}` accepts the same fields and returns the updated budget. `DELETE /budgets/{id}` returns `{ "success": true }`. Spent amounts are derived by the frontend from matching expense transactions.

## Reports

`GET /reports/monthly?month=10&year=2026` returns:

```json
{
  "month": 10,
  "year": 2026,
  "income": 52000,
  "expense": 12450,
  "balance": 39550,
  "savingsRate": 76,
  "categories": [{ "category": "Food", "amount": 4200, "percentage": 34, "color": "#2f8c6a" }],
  "comparison": { "incomeChange": 8.4, "expenseChange": -4.2 }
}
```

`savingsRate` and category `percentage` are integer percentages. Comparison values are signed percentage changes versus the preceding month; `color` is optional and may be assigned by the client.

`GET /reports/category-summary?month=10&year=2026` returns `{ "items": [{ "category": "Food", "amount": 4200, "percentage": 34, "color": "#2f8c6a" }] }`.

`GET /reports/trend` returns `{ "items": [{ "month": "May", "monthNumber": 5, "year": 2026, "income": 50000, "expense": 29000 }] }`. The dashboard uses the latest six months. Optional `month` and `year` query parameters may be ignored.

## Suggestions and Goals

`GET /suggestions/savings` returns `{ "items": [{ "id": 1, "title": "Review subscriptions", "description": "Check recurring services.", "impact": "Find easy wins", "category": "Monthly check-in" }] }`.

`GET /suggestions/investments` returns `{ "items": [{ "id": 1, "title": "Emergency fund", "description": "Keep essential expenses accessible.", "riskLevel": "Low", "horizon": "Immediate priority" }] }`. Risk levels: `Low`, `Moderate`, `High`.

`GET /goals` returns an array of `{ "id": 1, "name": "Emergency cushion", "targetAmount": 100000, "currentAmount": 42000, "deadline": "2027-03-10" }`.

`POST /goals` accepts `{ "name": "Emergency cushion", "targetAmount": 100000, "currentAmount": 0, "deadline": "2027-03-10" }` and returns the created goal. `PUT /goals/{id}` accepts those fields and returns the updated goal. `DELETE /goals/{id}` returns `{ "success": true }`.

## Notifications

`GET /notifications` returns an array of `{ "id": 1, "title": "Budget getting close", "message": "Food is at 80% of its limit.", "createdAt": "2026-10-10T10:30:00Z", "read": false, "severity": "warning" }`. Severity values are `warning`, `positive`, or `info`.

`PUT /notifications/{id}/read` marks the item read and returns the updated notification.

## User Profile

`GET /users/profile` returns `{ "id": 1, "name": "Aarav Mehta", "email": "aarav@example.com", "currency": "INR" }`.

`PUT /users/profile` accepts profile changes `{ "name": "Aarav Mehta", "email": "aarav@example.com", "currency": "INR" }` and returns the updated profile. Password changes use the same endpoint with `{ "currentPassword": "old-password", "newPassword": "new-password" }`; the response is the profile object and must not include password fields. Supported currency codes in the UI are `INR`, `USD`, `EUR`, and `GBP`.
