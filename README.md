# 🚀 Generic DAO Framework (ORM-Swing)

[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://java.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Swing](https://img.shields.io/badge/Swing-Desktop-blueviolet?style=for-the-badge)](#)

A powerful, lightweight **Full-Stack Java Framework (JDK 17+)** designed to drastically accelerate Desktop application development. It seamlessly binds your PostgreSQL database to modern, dynamically generated Swing User Interfaces using Java Reflection, custom annotations, FlatLaf, and MigLayout, completely eliminating boilerplate code!

---

## 🛠️ Prerequisites

Before you begin, ensure you have the following installed:
* **Java Development Kit (JDK) 17** or higher.
* **PostgreSQL** database server.
* Relevant `.jar` libraries for dependencies (JDBC Driver, FlatLaf, MigLayout).

---

## ⚙️ Configuration

The framework manages database connections via a `.env` file located at the root of your project.

Create a `.env` file in the root directory and define the following variables:

```properties
# .env file

DB_DRIVER=org.postgresql.Driver
DB_URL=jdbc:postgresql://localhost:5432/your_database_name
DB_USER=your_username
DB_PASSWORD=your_password
```

The `ConfigReader` will automatically parse this file to establish connections using the `DBConnection` manager.

---

## 📦 Installation

To use this framework, you need to place the required `.jar` dependencies inside a `lib/` folder at the root of your project.

### Required Libraries
1. **PostgreSQL JDBC Driver:** For database connectivity.
2. **MigLayout (`miglayout-core`, `miglayout-swing`):** For advanced grid-based UI layouts.
3. *(Optional but recommended)* **FlatLaf:** For a modern Look & Feel.

After placing the dependencies in the `lib/` folder, you can compile and package the framework using the included Windows batch script:

```cmd
.\build.bat
```

This will generate `GenericDAO.jar` inside the `dist/` folder, which you can then include in your project's build path alongside the `lib/` contents.

---

## 🚀 Quick Start

Here is how you can map a database table to a modern Swing GUI in just a few steps!

### Step 1: Define an Entity

Use custom annotations to map your Java class to the database table and configure the UI generation.

```java
import com.framework.annotation.*;

@Table(name = "students")
public class Student {

    @Id(autoIncrement = true)
    @Column(name = "id")
    private int id;

    @Required(message = "Student name is mandatory")
    @FormField(label = "Full Name")
    @Column(name = "full_name")
    private String fullName;

    @Column(name = "age")
    private int age;

    // A Many-To-One Relationship example
    @JoinColumn(name = "department_id", eager = true)
    @FormField(label = "Department")
    private Department department;

    // Getters and Setters ...
}
```

### Step 2: Initialize Repositories

Create a Repository for your entity and optionally register it with the `RepositoryRegistry` (useful for resolving foreign key dependencies automatically).

```java
import com.framework.repository.AbstractRepository;
import com.framework.repository.RepositoryRegistry;

public class StudentRepository extends AbstractRepository<Student> {
    public StudentRepository() {
        super(Student.class);
    }
}

// In your application startup:
RepositoryRegistry.register(Student.class, new StudentRepository());
// Register Department repository as well...
```

### Step 3: Render the UI

Generate both a Data Grid (JTable) and a fully functional CRUD Form in just two lines of code!

```java
import com.framework.swing.SwingFormBuilder;
import com.framework.swing.SwingTablePanel;
import javax.swing.JPanel;
import javax.swing.JFrame;

// ... inside your JFrame setup ...

StudentRepository repo = new StudentRepository();

// 1. Generate a smart data grid
JPanel tablePanel = SwingTablePanel.buildTablePanel(Student.class, repo);

// 2. Generate a CRUD form with validation and Save/Update/Delete buttons
JPanel formPanel = new JPanel();
SwingFormBuilder.buildForm(
    formPanel,
    Student.class,
    repo,
    true, true, true, // Enable Save, Update, Delete buttons
    null, null,
    () -> System.out.println("Form Action Completed!")
);

// Add these panels to your frame
frame.add(tablePanel, BorderLayout.CENTER);
frame.add(formPanel, BorderLayout.SOUTH);
```

---

## 📖 Annotation Reference

The core power of the framework comes from its custom annotations.

| Annotation | Target | Description |
| :--- | :--- | :--- |
| `@Table` | Class | Specifies the name of the database table to which the entity is mapped (`name`). |
| `@Column` | Field | Maps the field to a standard database column (`name`). |
| `@Id` | Field | Marks the field as the primary key. Can be configured with `autoIncrement = true`. |
| `@JoinColumn` | Field | Defines a Many-To-One relationship (Foreign Key). ComboBoxes are automatically generated in the UI for these fields. |
| `@Required` | Field | UI Validation: Marks the field as mandatory. Displays a custom `message` if empty during form submission. |
| `@FormField` | Field | UI Customization: Overrides the default label text for the input field in the generated Swing form (`label`). |

---

## 🏗️ Architecture

The framework is built with a clean, modular structure under `src/com/framework/`:

- 📁 **`annotation/`**: Contains the custom ORM and UI annotations (`@Table`, `@Id`, `@Required`, etc.).
- 📁 **`connection/`**: Manages the PostgreSQL connection pool using `DBConnection` and reads the `.env` file via `ConfigReader`.
- 📁 **`dao/`**: Core ORM engine (`GenericDAO`) using Java Reflection to generate dynamic SQL for CRUD and batch operations.
- 📁 **`repository/`**: Abstraction layer implementing the Repository Pattern (`AbstractRepository`) and a global `RepositoryRegistry`.
- 📁 **`swing/`**: The dynamic UI engine (`SwingFormBuilder`, `SwingTablePanel`) that auto-generates forms and grids based on entity metadata.
- 📁 **`utils/`**: Helper utilities for Reflection (`ReflectionUtils`) and dynamic server/client side filtering (`Filtre`).
