# BookstoreManagement
Using Java and Spring Boot with MySQL database.

## Docker Setup

We have dockerized the application to make it easy to deploy/run in any environment. 

### Prerequisites
- [Docker](https://docs.docker.com/get-docker/) installed on your machine.
- [Docker Compose](https://docs.docker.com/compose/install/) (usually comes bundled with Docker Desktop).

### Option 1: Run with Docker Compose (Recommended)
This options spins up both the **Spring Boot application** and a **MySQL database** automatically, configures the network/connection between them, and ensures the database is fully initialized before the application starts.

1. Navigate to the project root directory.
2. Run the following command:
   ```bash
   docker compose up --build -d
   ```
3. The Bookstore Management web application will be accessible at:
   [http://localhost:8282](http://localhost:8282)
4. To stop the containers, run:
   ```bash
   docker compose down
   ```

### Option 2: Build & Run the Application Docker Image Standalone
If you already have a MySQL database running separately and just want to run the Spring Boot application container:

1. **Build the Docker Image**:
   ```bash
   docker build -t bookstore-management .
   ```

2. **Run the Container**:
   Pass your database connection details as environment variables so the container can connect to your external database:
   ```bash
   docker run -d \
     -p 8282:8282 \
     -e SPRING_DATASOURCE_URL=jdbc:mysql://<DATABASE_HOST>:<PORT>/bookmanager?serverTimezone=UTC \
     -e SPRING_DATASOURCE_USERNAME=<USERNAME> \
     -e SPRING_DATASOURCE_PASSWORD=<PASSWORD> \
     --name bookstore-app \
     bookstore-management
   ```
