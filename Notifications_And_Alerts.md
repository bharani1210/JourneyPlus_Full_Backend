I am building a backend module for a project called:

"JourneyPlus – Corporate Travel & Expense Management System"

I want to implement:

👉 Notifications & Alerts Module

Use a standard layered architecture:
- Controller
- Service
- Service Implementation
- Repository (Spring Data JPA)
- DTOs
- Entity

Tech Stack:
- Java Spring Boot
- Spring Data JPA
- REST APIs
- MySQL/PostgreSQL

------------------------------------------------------
✅ MODULE OBJECTIVE
------------------------------------------------------

This module handles:
- System-generated and manual notifications
- Alerts for trip approvals, expenses, advances, compliance issues
- User-specific notification visibility
- Mark as read / dismiss functionality

------------------------------------------------------
✅ ENTITY DESIGN
------------------------------------------------------

Create the following entity:

Notification:
- notificationId (Primary Key)
- userId (Target user)
- message (Text)
- category (TripRequest / Advance / ExpenseClaim / PolicyException / Compliance)
- status (Unread / Read / Dismissed)
- createdDate

Use proper JPA annotations.

------------------------------------------------------
✅ DTO REQUIREMENTS
------------------------------------------------------

1. NotificationRequestDTO:
   - userId
   - message
   - category

2. NotificationResponseDTO:
   - notificationId
   - userId
   - message
   - category
   - status
   - createdDate

------------------------------------------------------
✅ ROLE-BASED ACCESS CONTROL
------------------------------------------------------

Define roles:
- EMPLOYEE
- MANAGER
- FINANCE
- COMPLIANCE
- ADMIN

Access Rules:

CREATE Notification (POST):
- MANAGER, FINANCE, COMPLIANCE, ADMIN ✅
- EMPLOYEE ❌

VIEW Notifications (GET):
- All roles ✅
- BUT only their own notifications (user-level filtering)
- ADMIN can view all notifications

UPDATE Notification (PUT - mark as read):
- All users can update ONLY their own notifications

DELETE Notification (DELETE - dismiss):
- All users can dismiss ONLY their own notifications
- ADMIN can delete any notification

------------------------------------------------------
✅ API ENDPOINTS
------------------------------------------------------

1. Create Notification
POST /api/notifications

2. Get User Notifications
GET /api/notifications

(Query Params: status, category, pagination)

3. Get Notification by ID
GET /api/notifications/{id}

4. Mark as Read
PUT /api/notifications/{id}/read

5. Dismiss Notification (Soft Delete)
DELETE /api/notifications/{id}

6. Get Unread Count
GET /api/notifications/unread-count

------------------------------------------------------
✅ BUSINESS LOGIC
------------------------------------------------------

Implement the following logic:

1. Creation:
- Validate role before creating notification
- Auto-set:
  - status = "Unread"
  - createdDate = current timestamp

2. Fetch Notifications:
- Apply row-level filtering:
  - userId = logged-in user
- Admin bypasses filtering

3. Mark as Read:
- Validate ownership
- Change status → "Read"

4. Dismiss Notification:
- Do NOT hard delete
- Change status → "Dismissed"

5. Unread Count:
- Count notifications where:
  status = Unread AND userId = logged-in user

------------------------------------------------------
✅ AUTOMATED NOTIFICATION TRIGGERS (IMPORTANT)
------------------------------------------------------

Simulate or include methods for system-generated notifications:

Trigger scenarios:
- Trip Approved → Notify Employee
- Expense Submitted → Notify Manager
- Expense Approved → Notify Finance + Employee
- Reimbursement Processed → Notify Employee
- Policy Exception → Notify Compliance
- Advance Pending → Notify Employee
- Budget Overrun → Notify Manager + Finance

Create a service method:

generateSystemNotification(eventType, userId, message)

------------------------------------------------------
✅ IMPLEMENTATION EXPECTATION
------------------------------------------------------

Generate full code for:

1. Notification Entity (JPA)
2. DTOs (Request & Response)
3. Repository interface (JpaRepository)
4. Service interface
5. Service Implementation:
   - Role validation
   - Ownership validation
   - Business logic
6. Controller:
   - REST endpoints
   - Proper request/response mapping

------------------------------------------------------
✅ EXCEPTION HANDLING
------------------------------------------------------

Include:
- ResourceNotFoundException
- UnauthorizedAccessException

------------------------------------------------------
✅ BEST PRACTICES
------------------------------------------------------

- Use enums for Category and Status
- Use LocalDateTime for timestamps
- Follow clean code principles
- Add comments explaining logic
- Keep methods modular and readable

------------------------------------------------------
✅ BONUS FEATURES
------------------------------------------------------

Also include:
- Pagination support (Pageable)
- Sorting (by date)
- Filter by category and status

------------------------------------------------------
✅ OUTPUT FORMAT
------------------------------------------------------

Provide full working code with:
- Proper package structure
- Clean separation of layers

DO NOT skip any layer.
Ensure code is ready to integrate into a real Spring Boot project.
