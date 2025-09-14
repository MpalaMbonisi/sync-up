## SyncUp API

A collaborative task management REST API built with Spring Boot, designed to help teams organize and manage tasks efficiently through shared task lists.

### 📋 Overview

SyncUp is a robust task collaboration application that allows users to create task lists, manage tasks, and collaborate with team members. The API provides comprehensive functionality for user authentication, task list management, task operations, and collaboration features.

### 🏗️ Architecture & Tech Stack

- **Framework**: Spring Boot 3.5.5
- **Language**: Java 21
- **Database**: PostgreSQL 15
- **Security**: JWT Authentication with Spring Security
- **Build Tool**: Maven
- **Containerization**: Docker
- **Testing**: JUnit, Spring Boot Test, Testcontainers
- **Documentation**: Postman Collection included

### 🗄️ Database Schema

```
┌─────────────────┐       ┌──────────────────┐       ┌─────────────────┐
│     Account     │       │    Task_Lists    │       │   Task_Items    │
├─────────────────┤       ├──────────────────┤       ├─────────────────┤
│ id (PK)         │       │ id (PK)          │       │ id (PK)         │
│ username (UQ)   │◄──────┤ owner_id (FK)    │       │ description     │
│ first_name      │       │ title            │       │ completed       │
│ last_name       │       │                  │       │ tasklist_id (FK)│
│ email (UQ)      │       │                  │       │                 │
│ password        │       │                  │       │                 │
└─────────────────┘       └──────────────────┘       └─────────────────┘
        │                          │                          │
        │                          │                          │
        │         ┌────────────────────────────────────┐      │
        └─────────┤    Collaborators (M:N)             ├──────┘
                  │  task_lists_id + collaborators_id  │
                  └────────────────────────────────────┘
```

**Relationships:**
- Users can own multiple task lists (1:N)
- Users can collaborate on multiple task lists (M:N)
- Task lists contain multiple tasks (1:N)
- Unique constraints: `(owner_id, title)` for task lists

### 🚀 Features

#### Authentication
- User registration with validation
- JWT-based authentication
- Secure password hashing (BCrypt)

#### Task List Management
- Create personal task lists
- View all accessible lists (owned + collaborated)
- Update list titles
- Delete lists (owner only)

#### Collaboration
- Add collaborators to lists
- Remove collaborators (owner only)
- View all collaborators
- Access control for list operations

#### Task Management
- Create tasks within lists
- Update task status (completed/incomplete)
- Update task descriptions
- View individual tasks
- Prevent duplicate task descriptions per list

### 🔧 CI/CD Pipeline

#### Git Hooks (Pre-commit)
- **Trailing whitespace** - Removes trailing spaces
- **End-of-file fixer** - Ensures files end with newlines
- **YAML validation** - Validates YAML syntax
- **Merge conflict detection** - Prevents commits with conflict markers
- **Conventional commits** - Enforces commit message standards

#### Git Hooks (Pre-push)
- **Maven verify** - Runs all tests before pushing
- **Code quality checks** - Ensures code standards

#### GitHub Actions Workflow
The CI/CD pipeline includes the following jobs:

1. **Code Checkout** - Retrieves source code
2. **JDK 21 Setup** - Configures Java environment with Temurin distribution
3. **Maven Build & Test** - Compiles and runs comprehensive test suite
4. **Docker Image Build** - Creates containerized application
5. **Test Reporting** - Generates JUnit test reports
6. **Security Scanning** - Trivy vulnerability scanning (HIGH/CRITICAL)
7. **Image Packaging** - Compresses Docker image for deployment
8. **AWS Lightsail Deployment** - Automated deployment to cloud instance

**Deployment Features:**
- Zero-downtime deployment with health checks
- Automatic image cleanup (retains last 3 versions)
- Secure environment variable injection
- Docker Compose orchestration with PostgreSQL

### 🧪 Testing Strategy

Built following **Test-Driven Development (TDD)** principles:

- **Unit Tests** - Comprehensive service and repository testing
- **Integration Tests** - End-to-end API testing with Testcontainers
- **Security Tests** - Authentication and authorization validation
- **Database Tests** - PostgreSQL integration with test containers

### 📡 API Endpoints

#### Authentication
- `POST /auth/register` - User registration
- `POST /auth/login` - User authentication

#### Task Lists
- `GET /list/all` - Get all accessible lists
- `POST /list/create` - Create new task list
- `GET /list/{id}` - Get specific list
- `PATCH /list/{id}/title/update` - Update list title
- `DELETE /list/{id}` - Delete list

#### Collaboration
- `POST /list/{id}/collaborator/add` - Add collaborators
- `GET /list/{id}/collaborator/all` - List collaborators
- `DELETE /list/{id}/collaborator/remove` - Remove collaborator

#### Tasks
- `POST /list/{listId}/task/create` - Create task
- `GET /list/{listId}/task/{taskId}` - Get task details
- `PATCH /list/{listId}/task/{taskId}/status` - Update task status
- `PATCH /list/{listId}/task/{taskId}/description` - Update task description

#### Health Check
- `GET /actuator/health` - Application health status

###  Quick Start

####  Testing with Postman

Import the included `SyncUp API.postman_collection.json` file into Postman to test all endpoints. The collection includes:

- Pre-configured requests for all endpoints
- Automatic token management
- Example request/response data
- Environment variables setup

###  Infrastructure

**Current Deployment:**
- **Instance**: AWS Lightsail
- **Specifications**: 1GB RAM, 2 vCPUs, 40GB SSD
- **Services**: Application + PostgreSQL via Docker Compose

### 🔮 Future Roadmap

#### Frontend Development
- **Framework**: Angular
- **Architecture**: Polyrepo approach (separate repositories)
- **Hosting**: AWS S3 for static site hosting
- **Domain**: Custom domain with AWS Route 53

#### Infrastructure Migration
- **Backend**: Migrate from Lightsail to EC2 for better scalability
- **Database**: Consider AWS RDS for managed PostgreSQL
- **Portfolio Integration**: Personal portfolio site connected via Route 53

#### Development Improvements
- **Commit Standards**: Enhanced conventional commit messages
- **Code Review**: AI-assisted code reviews and mentor feedback
- **Documentation**: API documentation with OpenAPI/Swagger
- **Monitoring**: Application monitoring and logging improvements

### Some Technical Highlights I Want to Share

**The authentication flow** - I'm particularly proud of how I implemented JWT token management. The tokens are stateless, include proper expiration, and the security configuration handles everything gracefully.

**Database query optimization** - I used custom JPQL queries for the collaboration features. The `findByIdAndUserHasAccess` method was tricky to get right - it checks both ownership and collaboration in a single query.

**Error handling** - Built a comprehensive global exception handler that provides meaningful error messages without exposing sensitive information.

**Access control** - Every endpoint properly validates that users can only access their own data or data they're allowed to collaborate on.

### Lessons Learned

Building this taught me a lot about:
- **TDD discipline** - Writing tests first really does lead to better design
- **CI/CD complexity** - Setting up proper automation is harder than it looks but so worth it
- **Database relationships** - Many-to-many relationships need careful thought about access control
- **JWT security** - Stateless authentication has its own challenges and considerations
- **Docker deployment** - Containerization makes deployment consistent but adds complexity

### Thanks for Checking It Out!

This project represents my journey into professional backend development. It's not just code - it's a demonstration of modern development practices, from TDD to automated deployment.

If you try out the API or have any feedback, I'd love to hear from you!

### 👨‍💻 Author

**Mbonisi Mpala**
- LinkedIn: [Mbonisi Mpala](https://www.linkedin.com/in/mbonisi-mpala/)

---

*Built with ❤️ using Spring Boot and AWS*
