I am building a backend module for a project called "JourneyPlus – Corporate Travel & Expense Management System".

I want you to generate a complete implementation for the module:

👉 Travel Analytics & Reporting Module

Use a standard layered architecture:
- Controller
- Service
- ServiceImpl
- Repository (JPA)
- DTOs
- Entities

Tech Stack:
- Java Spring Boot
- Spring Data JPA
- REST APIs
- MySQL/PostgreSQL compatible

---------------------------------------
✅ REQUIREMENTS
---------------------------------------

The module is responsible for:
- Generating travel analytics reports
- Providing dashboards for:
  - Travel spend
  - Budget utilisation
  - Policy exception rate
  - Advance settlement rate
  - Top travellers
  - Spend by category

---------------------------------------
✅ ENTITY DESIGN
---------------------------------------

Create the following Entity:

TravelReport:
- reportId (Primary Key)
- scope (Department / Grade / Destination / Period)
- tripCount
- totalSpend
- avgCostPerTrip
- advanceSettlementRate
- policyExceptionRate
- budgetUtilisation
- generatedDate

Use JPA annotations properly.

---------------------------------------
✅ DTO REQUIREMENTS
---------------------------------------

Create DTO classes:

1. ReportRequestDTO:
   - scope
   - startDate
   - endDate
   - departmentId
   - gradeId
   - destination

2. ReportResponseDTO:
   - reportId
   - scope
   - tripCount
   - totalSpend
   - avgCostPerTrip
   - advanceSettlementRate
   - policyExceptionRate
   - budgetUtilisation
   - generatedDate

---------------------------------------
✅ ROLE-BASED ACCESS REQUIREMENTS
---------------------------------------

Implement role-based logic:

Roles:
- EMPLOYEE
- MANAGER
- FINANCE
- COMPLIANCE
- ADMIN

Access rules:
- EMPLOYEE → can only VIEW their own report data
- MANAGER → can generate reports for their department
- FINANCE → full access to financial reports
- COMPLIANCE → access to policy exception reports
- ADMIN → full access (create, delete, manage all reports)

Use role-based validation in Service layer.

---------------------------------------
✅ API ENDPOINTS
---------------------------------------

1. Generate Report
POST /api/reports

2. Get Report by ID
GET /api/reports/{id}

3. Get Reports (with filters)
GET /api/reports

4. Delete Report
DELETE /api/reports/{id}

5. Top Travellers
GET /api/reports/top-travellers

6. Spend by Category
GET /api/reports/spend-by-category

7. Budget Utilisation
GET /api/reports/budget-utilisation

---------------------------------------
✅ BUSINESS LOGIC
---------------------------------------

Simulate or implement aggregation logic:

- tripCount → count from TripRequest
- totalSpend → sum of ExpenseLine
- avgCostPerTrip → totalSpend / tripCount
- advanceSettlementRate → based on AdvanceSettlement
- policyExceptionRate → from PolicyException
- budgetUtilisation → spend vs allocated budget

You can mock repository calls if needed but structure should be realistic.

---------------------------------------
✅ IMPLEMENTATION EXPECTATION
---------------------------------------

Generate:

1. Entity class (TravelReport)
2. DTO classes
3. Repository interface (JpaRepository)
4. Service interface
5. Service implementation (with role validation + logic)
6. Controller class with endpoints
7. Proper request/response mapping
8. Exception handling (basic)
9. Use clean architecture and best practices

---------------------------------------
✅ BONUS (IMPORTANT)
---------------------------------------

- Add comments explaining logic
- Show where role-based filtering happens
- Use meaningful method names
- Follow real-world enterprise coding style

---------------------------------------

Output complete working code in structured format.
Do NOT skip any layer.
