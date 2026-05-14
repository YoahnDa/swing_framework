# Generic DAO Framework

A lightweight Java framework designed to simplify database operations through a generic Data Access Object (DAO), the Repository pattern, and utilities for automatically generating Swing user interfaces based on entity models.

## Features

- **Reflection-Based CRUD Operations:** Automatically handles Insert, Update, Delete, and Select operations without writing boilerplate SQL queries for each entity.
- **Custom Annotations:** Easy-to-use annotations for mapping Java classes to database tables:
  - `@Table(name = "table_name")`: Maps a class to a specific table.
  - `@Column(name = "column_name")`: Maps a field to a table column.
  - `@Id`: Marks a field as the primary key (supports auto-increment configuration).
  - `@JoinColumn`: Defines relationships (foreign keys) between entities.
  - `@Required`: Enforces validation rules on entity fields.
  - `@FormField`: Customizes the display label in generated Swing forms.
- **Repository Pattern:** Provides an `AbstractRepository<T>` to extend, offering standard data access methods out-of-the-box and the ability to add custom domain-specific queries.
- **Automatic Swing UI Generation:** The `SwingFormBuilder` utility uses reflection to dynamically create Swing forms (input fields, combo boxes for foreign keys, spinners) based on your entity classes and annotations.
- **Database Connection Management:** Includes a basic connection manager (`DBConnection`) that reads configuration from standard properties.
- **Batch Processing:** Support for batch inserts, updates, and deletes for improved performance when handling multiple records.
- **Flexible Queries:** Supports custom SQL queries and dynamic filtering using the `Filtre` utility class.

## Architecture & Package Structure

The framework is organized into the following packages under `src/com/framework/`:

- **`annotation`**: Contains all custom annotations (`@Table`, `@Column`, `@Id`, etc.) used for Object-Relational Mapping and form generation.
- **`connection`**: Handles database connections and configuration reading (`DBConnection`, `ConfigReader`).
- **`dao`**: Contains the core `GenericDAO` class that executes all database operations using JDBC and Java Reflection.
- **`repository`**: Provides the `AbstractRepository` class for the Repository pattern, along with a `RepositoryRegistry`.
- **`swing`**: Utilities like `SwingFormBuilder` and `SwingTablePanel` for rapid desktop UI development.
- **`utils`**: Helper classes for Reflection (`ReflectionUtils`) and building dynamic queries (`Filtre`).

## Build Instructions

The project includes a Windows batch script (`build.bat`) to compile the source code and package it into a JAR file.

To build the framework on Windows:

1. Open a Command Prompt or Terminal.
2. Navigate to the root directory of the project.
3. Run the following command:
   ```cmd
   .\build.bat
   ```
4. Upon successful compilation, the built JAR (`GenericDAO.jar`) will be available in the `dist` directory.

## Usage Examples

### 1. Mapping an Entity

```java
import com.framework.annotation.*;

@Table(name = "users")
public class User {
    @Id(autoIncrement = true)
    @Column(name = "id")
    private int id;

    @Required(message = "Name cannot be empty")
    @FormField(label = "Full Name")
    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    // Getters and Setters...
}
```

### 2. Creating a Repository

```java
import com.framework.repository.AbstractRepository;

public class UserRepository extends AbstractRepository<User> {
    public UserRepository() {
        super(User.class);
    }

    // Add custom repository methods here if needed
}
```

### 3. Basic CRUD Operations

```java
UserRepository repo = new UserRepository();

// Create
User newUser = new User();
newUser.setName("John Doe");
newUser.setEmail("john@example.com");
repo.save(newUser);

// Read
User user = repo.findById(1);
List<User> allUsers = repo.findAll();

// Update
user.setEmail("john.doe@example.com");
repo.save(user);

// Delete
repo.delete(user);
```

### 4. Generating a Swing Form

```java
import com.framework.swing.SwingFormBuilder;
import javax.swing.JPanel;

JPanel formPanel = new JPanel();
UserRepository repo = new UserRepository();

// Automatically builds a form with Save, Update, and Delete buttons
SwingFormBuilder.buildForm(
    formPanel,
    User.class,
    repo,
    true,  // showSave
    true,  // showUpdate
    true,  // showDelete
    null,  // customButtons
    null,  // existingObject
    () -> System.out.println("Operation successful!") // onSuccess callback
);
```