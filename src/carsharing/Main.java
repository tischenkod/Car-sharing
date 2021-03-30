package carsharing;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static carsharing.MenuResult.*;
import static java.lang.String.format;

public class Main {
    // JDBC driver name and database URL
    static final String JDBC_DRIVER = "org.h2.Driver";
    static String DB_URL = "jdbc:h2:./src/carsharing/db/";
    static Connection conn;
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

        DB_URL += args.length == 2 && args[0].equals("-databaseFileName") ? args[1] : "carsharing";
        try {
            Class.forName (JDBC_DRIVER);
            conn = DriverManager.getConnection (DB_URL);
            conn.setAutoCommit(true);
            Statement st = conn.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS company ( \n" +
                    "id INTEGER PRIMARY KEY AUTO_INCREMENT, \n" +
                    "name VARCHAR NOT NULL UNIQUE);" +
                    "CREATE TABLE IF NOT EXISTS car (" +
                    "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                    "name varchar UNIQUE NOT NULL," +
                    "company_id INTEGER NOT NULL," +
                    "CONSTRAINT fk_company FOREIGN KEY (company_id) REFERENCES company(id));" +
                    "CREATE TABLE IF NOT EXISTS customer (" +
                    "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                    "name varchar UNIQUE NOT NULL," +
                    "rented_car_id INTEGER," +
                    "CONSTRAINT fk_rented_car FOREIGN KEY (rented_car_id) REFERENCES car(id));";
            st.executeUpdate(sql);
            st.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        ListMenuItem menu = new ListMenuItem()
                .add(new ListMenuItem(1, "Log in as a manager")
                        .add(new DynamicMenuItem(1, "Company list", Main::companyList)
                            .setOnGetCaption(Main::getCompanyCaption))
                        .add(new ActionMenuItem(2, "Create a company", Main::createACompany))
                        .add(new ActionMenuItem(0, "Back", (ignored) -> MR_BACK))
                )
                .add(new DynamicMenuItem(2, "Log in as a customer", Main::customerList)
                        .setOnGetCaption(Main::getCustomerCaption))
                .add(new ActionMenuItem(3, "Create a customer", Main::createACustomer))
                .add(new ActionMenuItem(0, "Exit", (ignored) -> MR_BACK));

        menu.enter();
        try {
            conn.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private static String getCustomerCaption(MenuItem sender) {
        return ((ListMenuItem) sender).size() > 0 ? "Choose a customer:" : "The customer list is empty!";
    }

    private static void customerList(MenuItem sender) {
        DynamicMenuItem dynamicMenuItem = (DynamicMenuItem) sender;
        String sql = "SELECT id, name FROM customer";
        dynamicMenuItem.clear();
        try (Statement st = conn.createStatement(); ResultSet result = st.executeQuery(sql)){
            int index = 1;
            while (result.next()) {
                dynamicMenuItem.add(new ListMenuItem(index++, result.getString("name"))
                        .add(new DynamicMenuItem(1, "Rent a car", Main::companiesForRentACar)
//                                .setOnGetCaption(Main::getCompanyCaption)
                                .setData(Map.of("customer_id", result.getInt("id"))))
                        .add(new ActionMenuItem(2, "Return a rented car", Main::returnARentedCar).setData(Map.of("customer_id", result.getInt("id"))))
                        .add(new ActionMenuItem(3, "My rented car", Main::myRentedCar).setData(Map.of("customer_id", result.getInt("id"))))
                        .add(new ActionMenuItem(0, "Back", (ignored) -> MR_BACK.stepCount(2)))
                        .setData(Map.of("customer_id", result.getInt("id")))
                );
            }
            if (dynamicMenuItem.size() > 0) {
                dynamicMenuItem.add(new ActionMenuItem(0, "Back", (ignored) -> MR_BACK));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private static void companiesForRentACar(MenuItem sender) {
        DynamicMenuItem dynamicMenuItem = (DynamicMenuItem) sender;
        dynamicMenuItem.clear();
        if (rentedCarInfo((int) sender.data.get("customer_id")) != null) {
            System.out.println("You've already rented a car!");
            return;
        }
        String sql = "SELECT id, name from company";
        try (Statement st = conn.createStatement(); ResultSet result = st.executeQuery(sql)) {
            int index = 1;
            while (result.next()) {
                Map<String, Object> info = new HashMap<>(sender.data);
                info.put("company_id", result.getInt("id"));
                info.put("company_name", result.getString("name"));
                dynamicMenuItem.add(new DynamicMenuItem(index++, result.getString("name"), Main::carsForRent)
                    .setOnGetCaption(Main::getCarsCaption)
                    .setData(info));
            }
            if (dynamicMenuItem.size() > 0) {
                dynamicMenuItem.add(new ActionMenuItem(0, "Back", (ignored) -> MR_BACK));
            } else {
                System.out.println("The company list is empty!");
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private static void carsForRent(MenuItem sender) {
        DynamicMenuItem dynamicMenuItem = (DynamicMenuItem) sender;
        String sql = format("SELECT id, name from car " +
                "WHERE company_id = %d "
                + "AND id NOT IN (SELECT rented_car_id FROM customer WHERE rented_car_id IS NOT NULL)"
                , sender.data.get("company_id"));
        try (Statement st = conn.createStatement(); ResultSet result = st.executeQuery(sql)){
            int index = 1;
            while (result.next()) {
                Map<String, Object> info = new HashMap<>(sender.data);
                info.put("car_id", result.getInt("id"));
                info.put("car_name", result.getString("name"));
                dynamicMenuItem.add(new ActionMenuItem(index++, result.getString("name"), Main::rentTheCar)
                        .setData(info));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private static MenuResult rentTheCar(MenuItem sender) {
        String sql = format("UPDATE customer " +
                "SET rented_car_id = %d " +
                "WHERE id = %d", sender.data.get("car_id"), sender.data.get("customer_id"));
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
            System.out.printf("You rented '%s'%n", sender.data.get("car_name"));
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return MR_BACK.stepCount(2);
    }

    private static MenuResult returnARentedCar(MenuItem sender) {
        Map<String, String> info = rentedCarInfo((Integer) sender.data.get("customer_id"));
        if (info != null && info.size() > 0) {
            String sql = format("UPDATE customer SET rented_car_id = NULL WHERE id = %d", sender.data.get("customer_id"));
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
                System.out.println("You've returned a rented car!");
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        } else {
            System.out.println("You didn't rent a car!");
        }
        return MR_NORMAL;
    }

    private static Map<String, String> rentedCarInfo(int carId) {
        String sql = format("SELECT car.name car, company.name company FROM customer, car, company " +
                "WHERE car.id = customer.rented_car_id " +
                "AND car.company_id = company.id " +
                "AND customer.id = %d", carId);
        Map<String, String> info = null;
        try (Statement st = conn.createStatement(); ResultSet result = st.executeQuery(sql)) {
            if (result.next()) {
                info = Map.of("car", result.getString("car"), "company", result.getString("company"));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return info;
    }

    private static MenuResult myRentedCar(MenuItem sender) {
        Map<String, String> info = rentedCarInfo((Integer) sender.data.get("customer_id"));

        if (info != null && info.size() > 0) {
            System.out.printf("Your rented car:%n" +
                    "%s%n" +
                    "Company:%n" +
                    "%s%n", info.get("car"), info.get("company"));
        } else {
            System.out.println("You didn't rent a car!");
        }
        return MR_NORMAL;
    }

    private static MenuResult createACustomer(MenuItem sender) {
        System.out.println("Enter the customer name:");
        String sql = format("INSERT INTO customer (name) VALUES ('%s')", scanner.nextLine());
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return MR_NORMAL;
    }

    private static String getCompanyCaption(MenuItem sender) {
        if (!(sender instanceof ListMenuItem)) {
            return null;
        }
        if (((ListMenuItem) sender).itemCount() > 0) {
            return "Company list:\n";
        } else {
            return "The company list is empty!";
        }
    }

    private static String getCarsCaption(MenuItem sender) {
        if (!(sender instanceof ListMenuItem)) {
            return null;
        }
        if (((ListMenuItem) sender).itemCount() > 0) {
            return "Choose a car:";
        } else {
            return format("No available cars in the '%s' company.", sender.data.get("company_name"));
        }
    }

    static void companyList(MenuItem sender) {
        DynamicMenuItem dynamicMenuItem = (DynamicMenuItem) sender;
        dynamicMenuItem.clear();
        String sql = "SELECT id, name FROM company ORDER BY id";
        try (Statement st = conn.createStatement(); ResultSet result = st.executeQuery(sql)){
            int index = 1;
            while (result.next()) {
                ListMenuItem listMenuItem = new ListMenuItem(index++, result.getString("name"));
                listMenuItem.add(new ActionMenuItem(1, "Car list", Main::printCarList).setData(Map.of("company_id", result.getInt("id"))));
                listMenuItem.add(new ActionMenuItem(2, "Create a car", Main::createACar).setData(Map.of("company_id", result.getInt("id"))));
                listMenuItem.add(new ActionMenuItem(0, "Back", (ignored) -> MR_BACK.stepCount(2)));
                listMenuItem.onGetCaption = (s) -> "'" + s.name + "' company";
                dynamicMenuItem.add(listMenuItem);
            }
            if (dynamicMenuItem.size() > 0) {
                dynamicMenuItem.add(new ActionMenuItem(0, "Back", (ignored) -> MR_BACK));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        System.out.println();
    }

    private static MenuResult createACar(MenuItem sender) {
        System.out.println("Enter the car name:");
        String sql = format("INSERT INTO car (name, company_id) VALUES ('%s', %d)", scanner.nextLine(), sender.data.get("company_id"));
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return MR_NORMAL;
    }

    private static MenuResult printCarList(MenuItem sender) {
        String sql = format("SELECT id, name FROM car WHERE company_id=%d ORDER BY id", sender.data.get("company_id"));
        try (Statement st = conn.createStatement(); ResultSet result = st.executeQuery(sql)){
            if (!result.next()) {
                System.out.println("The car list is empty!");
            } else {
                int index = 1;
                do {
                    System.out.printf("%d. %s%n", index++, result.getString("name"));
                } while (result.next());
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        System.out.println();
        return MR_NORMAL;
    }

    static MenuResult createACompany(MenuItem sender) {
        System.out.println("Enter the company name:");
        String sql = format("INSERT INTO company (name) VALUES ('%s')", scanner.nextLine());
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return MR_NORMAL;
    }
}