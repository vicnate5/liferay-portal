# Recreate Calendar App as calendar-collab

## Overview
Recreate the calendar application found in `modules/apps/calendar/` as a new app named `calendar-collab` using:
- **Backend**: Objects API instead of Service Builder
- **Frontend**: Modern React/TypeScript with Clay UI components instead of JSP
- **Testing**: Playwright test suite following existing patterns
- **Performance**: Performance testing and optimization
- **Security**: Security review and testing

## Current Calendar App Analysis

### Data Model (from service.xml)
The current calendar app has 4 main entities:
1. **Calendar** - Calendar containers with name, description, timezone, color, default flag
2. **CalendarBooking** - Events with title, description, location, start/end times, recurrence, reminders, invitations
3. **CalendarResource** - Links calendars to users/groups/sites
4. **CalendarNotificationTemplate** - Email notification templates

### Key Functionality (from Playwright tests)
- Create/edit/delete calendar events
- All-day and timed events
- Recurrence patterns (daily, weekly, monthly, yearly)
- Event invitations
- Multiple calendar views (Month, Day, Week)
- Timezone support
- Workflow integration
- Multi-language support (localized fields)
- Event reminders

## Implementation Plan

### Phase 1: Objects Backend Setup

#### 1.1 Create Implementation Module
Location: `modules/apps/calendar-collab/calendar-collab-impl/`

Create the implementation module following Liferay conventions:
- Module name: `calendar-collab-impl`
- Package structure: `com.liferay.calendar.collab.internal`
- Batch engine data location: `src/main/resources/com/liferay/calendar/collab/internal/batch/01-object-definition.batch-engine-data.json`

**Note**: Object definitions are created in the `-impl` module using batch engine data files, not a separate `-object-definitions` module. The file should be named `01-object-definition.batch-engine-data.json` following the pattern `{number}-object-definition.batch-engine-data.json`.

#### 1.2 Create Object Definitions
Location: `modules/apps/calendar-collab/calendar-collab-impl/src/main/resources/com/liferay/calendar/collab/internal/batch/01-object-definition.batch-engine-data.json`

Create Object Definitions for:
- **CalendarCollabCalendar** - Maps to current Calendar entity
  - Fields: name (localized text), description (localized text), timeZoneId (text), color (integer), defaultCalendar (boolean), enableComments (boolean), enableRatings (boolean), calendarResourceId (relationship)
  - Relationships: one-to-many with CalendarCollabEvent, many-to-one with CalendarCollabResource

- **CalendarCollabEvent** - Maps to current CalendarBooking entity
  - Fields: title (localized text), description (localized text), location (text), startTime (dateTime), endTime (dateTime), allDay (boolean), recurrence (text), firstReminder (long), firstReminderType (text), secondReminder (long), secondReminderType (text), status (integer), parentEventId (relationship), recurringEventId (relationship), calendarId (relationship)
  - Relationships: many-to-one with CalendarCollabCalendar, self-referential for recurrence

- **CalendarCollabResource** - Maps to current CalendarResource entity
  - Fields: code (text), name (localized text), description (localized text), active (boolean), classNameId (long), classPK (long), classUuid (text)
  - Relationships: one-to-many with CalendarCollabCalendar

- **CalendarCollabNotificationTemplate** - Maps to current CalendarNotificationTemplate entity
  - Fields: notificationType (text), notificationTypeSettings (text), notificationTemplateType (text), subject (text), body (text), calendarId (relationship)
  - Relationships: many-to-one with CalendarCollabCalendar

#### 1.3 Create Object Actions
- CRUD operations via Object Actions
- Custom actions for recurrence handling
- Invitation management actions

#### 1.4 Create REST APIs
Location: `modules/apps/calendar-collab/calendar-collab-rest-api/` and `calendar-collab-rest-impl/`

Create Headless REST APIs leveraging Objects REST framework:
- `/o/c/calendarcollabcalendars` - Calendar management
- `/o/c/calendarcollabevents` - Event management
- `/o/c/calendarcollabresources` - Resource management
- Custom endpoints for:
  - Recurrence pattern generation/parsing
  - Event invitations
  - Calendar view data (month/week/day views)

### Phase 2: Frontend Implementation

#### 2.1 Project Structure
Location: `modules/apps/calendar-collab/calendar-collab-web/`

```
src/main/resources/META-INF/resources/
├── js/
│   ├── components/
│   │   ├── CalendarView/
│   │   │   ├── MonthView.tsx
│   │   │   ├── WeekView.tsx
│   │   │   ├── DayView.tsx
│   │   │   └── AgendaView.tsx
│   │   ├── EventForm/
│   │   │   ├── EventFormModal.tsx
│   │   │   ├── RecurrenceSettings.tsx
│   │   │   ├── InvitationSettings.tsx
│   │   │   └── ReminderSettings.tsx
│   │   ├── CalendarList/
│   │   │   └── CalendarList.tsx
│   │   └── EventCard/
│   │       └── EventCard.tsx
│   ├── services/
│   │   ├── CalendarService.ts
│   │   ├── EventService.ts
│   │   └── RecurrenceService.ts
│   ├── utils/
│   │   ├── dateUtils.ts
│   │   ├── recurrenceUtils.ts
│   │   └── timezoneUtils.ts
│   └── main.tsx
├── css/
│   └── main.scss
└── view.jsp (minimal entry point)
```

#### 2.2 Main Components

**CalendarView Component** (`js/components/CalendarView/`)
- Use Clay components: `@clayui/layout`, `@clayui/navigation-bar`, `@clayui/button`
- Implement month/week/day views using modern calendar libraries (e.g., FullCalendar or custom)
- Responsive design following Clay patterns

**EventForm Component** (`js/components/EventForm/`)
- Use Clay form components: `@clayui/form`, `@clayui/date-picker`, `@clayui/time-picker`
- Localized fields using `frontend-js-web` localization
- Recurrence UI with Clay dropdowns and checkboxes
- Invitation selector using user/item selector APIs

**CalendarList Component** (`js/components/CalendarList/`)
- Use Frontend Data Set for calendar management table
- Clay table components with actions

#### 2.3 Services Layer

**EventService.ts** - Handle Object Entry CRUD operations
- Use Headless Object API client (`@liferay/object-admin-rest-client-js`)
- Methods: `getEvents()`, `createEvent()`, `updateEvent()`, `deleteEvent()`, `getEvent()`
- Handle recurrence expansion
- Handle timezone conversions

**CalendarService.ts** - Calendar management
- Methods: `getCalendars()`, `createCalendar()`, `updateCalendar()`, `deleteCalendar()`

**RecurrenceService.ts** - Recurrence pattern handling
- Parse/generate recurrence strings (RRULE format)
- Calculate recurring instances
- Handle recurrence exceptions

#### 2.4 Entry Point
- Minimal JSP (`view.jsp`) that loads React app
- Use `frontend-taglib-react` for React component mounting
- Portlet configuration via Clay modal

### Phase 3: Styling

#### 3.1 Clay Design System
- Use Clay CSS variables and utilities
- Follow Liferay design tokens
- Responsive breakpoints
- Dark mode support

#### 3.2 Custom Styles
Location: `css/main.scss`
- Calendar grid styling
- Event card styling
- View-specific styles
- Animation/transitions

### Phase 4: Testing

#### 4.1 Playwright Test Structure
Location: `modules/test/playwright/tests/calendar-collab-web/main/`

Create test files:
- `calendarEvent.spec.ts` - Event CRUD operations
- `calendarRecurrence.spec.ts` - Recurrence patterns
- `calendarInvitations.spec.ts` - Invitation functionality
- `calendarViews.spec.ts` - View switching and display
- `calendarTimezone.spec.ts` - Timezone handling

#### 4.2 Page Object Model
Location: `modules/test/playwright/pages/calendar-collab-web/`

- `CalendarCollabWidgetPage.ts` - Main page object (similar to existing `CalendarWidgetPage.ts`)
- `EventFormPage.ts` - Event form interactions
- `RecurrenceModalPage.ts` - Recurrence settings

#### 4.3 Test Coverage
Cover all functionality from existing calendar tests:
- Event creation (all-day, timed, with recurrence)
- Event editing (single event, entire series, following events)
- Event deletion
- Invitations
- Timezone handling
- Multi-language support
- Workflow integration
- Calendar management

#### 4.4 Comprehensive Test Review
Location: Review existing test suites to identify additional scenarios

After implementing basic Playwright test scenarios, conduct a comprehensive review of all existing calendar tests to identify additional testing scenarios:

**4.4.1 Review Poshi Tests (testFunctional)**
Location: `modules/apps/calendar/calendar-web-test/src/testFunctional/tests/`

Review all Poshi test cases (`.testcase` files) to identify scenarios:
- `CalendarPage.testcase` - View types and navigation
- `NewEvent.testcase` - Event creation scenarios
- `EventDetails.testcase` - Event viewing and details
- `CalendarScheduler.testcase` - Scheduler functionality
- `CalendarPermissions.testcase` - Permission and access control
- `CalendarStaging.testcase` - Staging environment scenarios
- `CalendarExportImport.testcase` - Export/import functionality
- `CalendarLocalization.testcase` - Localization scenarios
- `CalendarRSS.testcase` - RSS feed functionality
- `CalendarComment.testcase` - Comment functionality
- `CalendarConfiguration.testcase` - Configuration scenarios
- `ConfigurationTemplate.testcase` - Template configuration
- `NotificationTemplates.testcase` - Notification scenarios
- `Resources.testcase` - Resource management
- `ResourcesListView.testcase` - Resource list views
- `AddCalendar.testcase` - Calendar creation
- `ManageCalendar.testcase` - Calendar management
- `CalendarLookAndFeelConfiguration.testcase` - Look and feel
- `CalendarEmailTemplate.testcase` - Email templates
- `CalendarUpgrade.testcase` - Upgrade scenarios
- `AddCalendarAccessibility.testcase` - Accessibility features

**4.4.2 Review Integration Tests (testIntegration)**
Location: `modules/apps/calendar/calendar-test/src/testIntegration/java/com/liferay/calendar/`

Review Java integration tests to identify:
- Service layer test scenarios
- Persistence layer tests
- Search functionality tests
- Info item provider tests
- Upgrade path tests
- Change tracking tests
- Ratings integration tests
- Utility class tests

**4.4.3 Review Unit Tests (testUnit)**
Location: Check for `src/test/java/` directories in calendar modules

Review unit tests to identify:
- Individual component tests
- Utility function tests
- Business logic tests
- Edge case scenarios

**4.4.4 Test Scenario Documentation**
Create a comprehensive test scenario matrix documenting:
- All scenarios from Poshi tests
- All scenarios from integration tests
- All scenarios from unit tests
- Map each scenario to corresponding Playwright test
- Identify gaps where scenarios need new Playwright tests
- Prioritize scenarios by importance and frequency of use

**4.4.5 Additional Test Implementation**
Based on the review, implement additional Playwright tests for:
- Scenarios identified in Poshi tests not covered by basic scenarios
- Edge cases from integration tests
- Business logic validation from unit tests
- Accessibility scenarios
- Upgrade/migration scenarios
- Staging environment scenarios
- Export/import functionality
- RSS feed functionality
- Comment functionality
- Permission edge cases

### Phase 5: Performance Testing & Optimization

#### 5.1 Performance Benchmarks
- Establish baseline metrics from existing calendar app:
  - Page load times
  - API response times
  - Calendar view rendering (month/week/day with 100+ events)
  - Recurrence calculation performance
  - Search query performance
  - Bulk operations (create/update/delete multiple events)

#### 5.2 Performance Testing Tools
- Use Liferay's built-in performance monitoring
- Browser DevTools Performance profiling
- Lighthouse audits for frontend performance
- Load testing for API endpoints (using tools like JMeter or k6)
- Database query analysis for Objects queries

#### 5.3 Performance Optimization Areas
- **Frontend:**
  - Code splitting and lazy loading for calendar views
  - Virtual scrolling for large event lists
  - Memoization of expensive calculations (recurrence, timezone conversions)
  - Debouncing search inputs
  - Image/icon optimization
  - Bundle size optimization

- **Backend:**
  - Object Definition indexing strategy
  - Query optimization (avoid N+1 queries)
  - Caching strategies for frequently accessed data
  - Pagination for large result sets
  - Batch operations for bulk updates

#### 5.4 Performance Test Suite
Location: `modules/test/playwright/tests/calendar-collab-web/performance/`

Create performance tests:
- `calendarLoadPerformance.spec.ts` - Measure initial page load
- `calendarViewPerformance.spec.ts` - Measure view rendering with various event counts
- `recurrenceCalculationPerformance.spec.ts` - Measure recurrence expansion performance
- `apiResponseTimePerformance.spec.ts` - Measure API endpoint response times

### Phase 6: Security Review

#### 6.1 Security Checklist
- [ ] Input validation and sanitization
- [ ] XSS prevention (React's built-in escaping, but verify)
- [ ] CSRF protection (verify Liferay's built-in protection)
- [ ] SQL injection prevention (Objects API handles this, but verify)
- [ ] Authorization checks (permissions on Object Definitions)
- [ ] Authentication verification
- [ ] Secure data transmission (HTTPS)
- [ ] Sensitive data handling (user emails, personal information)
- [ ] File upload security (if applicable)
- [ ] API rate limiting considerations

#### 6.2 Security Testing
- **Static Analysis:**
  - Use Liferay's security scanning tools
  - ESLint security plugins for frontend code
  - SonarQube or similar for code quality and security

- **Dynamic Analysis:**
  - OWASP ZAP or Burp Suite for API security testing
  - Penetration testing for common vulnerabilities
  - Dependency scanning (npm audit, Snyk, etc.)

#### 6.3 Security Best Practices Implementation
- **Frontend:**
  - Sanitize user inputs before rendering
  - Use parameterized queries (via Objects API)
  - Implement Content Security Policy (CSP) headers
  - Validate all user inputs on client and server
  - Use secure storage for sensitive data (if needed)

- **Backend:**
  - Leverage Objects API permission system
  - Implement proper role-based access control (RBAC)
  - Validate all inputs at API boundaries
  - Log security events (failed auth attempts, permission denials)
  - Use Liferay's encryption for sensitive fields if needed

#### 6.4 Security Test Suite
Location: `modules/test/playwright/tests/calendar-collab-web/security/`

Create security tests:
- `authorizationTests.spec.ts` - Test permission enforcement
- `inputValidationTests.spec.ts` - Test XSS and injection prevention
- `csrfProtectionTests.spec.ts` - Verify CSRF protection
- `dataAccessTests.spec.ts` - Verify users can only access authorized data

### Phase 7: Migration Considerations

#### 7.1 Data Migration (Optional)
- Script to migrate existing Calendar data to Objects
- Map Service Builder entities to Object Definitions
- Preserve relationships and metadata

#### 7.2 Feature Parity Checklist
- [ ] Calendar CRUD
- [ ] Event CRUD
- [ ] Recurrence patterns
- [ ] Invitations
- [ ] Reminders
- [ ] Multiple views
- [ ] Timezone support
- [ ] Localization
- [ ] Workflow
- [ ] Permissions
- [ ] Search
- [ ] Export/Import (iCal)

## Technical Details

### Module Structure and Naming Conventions

**Module Naming Pattern:**
- Main app module: `calendar-collab` (with `app.bnd` and `build.gradle`)
- Implementation module: `calendar-collab-impl` (contains object definitions, batch engine data)
- REST API modules: `calendar-collab-rest-api` and `calendar-collab-rest-impl`
- Web module: `calendar-collab-web` (frontend React/TypeScript)
- Test module: `calendar-collab-test` (integration tests)
- Web test module: `calendar-collab-web-test` (Poshi functional tests, if needed)

**Package Naming:**
- Base package: `com.liferay.calendar.collab`
- Internal package: `com.liferay.calendar.collab.internal`
- Batch engine data: `com.liferay.calendar.collab.internal.batch`

**File Naming:**
- Object definition batch file: `01-object-definition.batch-engine-data.json`
- REST API OpenAPI spec: `rest-openapi.yaml`
- REST API config: `rest-config.yaml`

### Dependencies
- `@clayui/core`, `@clayui/form`, `@clayui/layout`, `@clayui/navigation-bar`, `@clayui/button`, `@clayui/date-picker`, `@clayui/time-picker`
- `@liferay/object-admin-rest-client-js` for Objects API
- `frontend-js-web` for Liferay utilities
- `frontend-data-set-web` for table views
- React 18+ with TypeScript

### Build Configuration
- Gradle build with `build.gradle` similar to `object-web` and `object-rest-impl`
- Node scripts for frontend build
- OSGi bundle configuration with `bnd.bnd` files

### Key Files to Reference
- `modules/apps/calendar/calendar-service/service.xml` - Data model reference
- `modules/apps/calendar/calendar-impl/` - Implementation patterns (if exists)
- `modules/apps/object/object-web/` - Objects frontend patterns
- `modules/apps/object/object-rest-api/` and `object-rest-impl/` - REST API patterns
- `modules/apps/frontend-data-set/frontend-data-set-impl/` - Object definition batch file example
- `modules/apps/cookies/cookies-impl/` - Object definition batch file example
- `modules/apps/site/site-cms-site-initializer/` - Modern React/Clay examples
- `modules/test/playwright/tests/calendar-web/` - Test patterns
- `modules/test/playwright/tests/object-web/` - Objects test patterns
- `modules/apps/calendar/calendar-web-test/src/testFunctional/` - Poshi test patterns
- `modules/apps/calendar/calendar-test/src/testIntegration/` - Integration test patterns

### Performance & Security Tools
- Liferay Performance Monitoring
- Browser DevTools Performance Profiler
- Lighthouse (for frontend performance)
- OWASP ZAP / Burp Suite (for security testing)
- ESLint security plugins
- npm audit / Snyk (for dependency vulnerabilities)

## Success Criteria
1. All existing calendar functionality works with Objects backend
2. Modern Clay UI matches Liferay design standards
3. Full Playwright test suite passes
4. No Service Builder dependencies
5. Performance equivalent or better than original
6. Performance benchmarks meet or exceed baseline metrics
7. Security review passes with no critical or high-severity vulnerabilities
8. All security tests pass
9. Comprehensive test review completed and all identified scenarios covered
