package util;

import annotations.Entity;
import annotations.Id;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AnnotationStrategyTest{
    private static List<Object> models1 = new ArrayList<>();
    private static List<Object> models2 = new ArrayList<>();

    @Entity(name = "Animals")
    class Animal {
        @Id
        public int animalId = 12345;
        public boolean fur = true;
        public Boolean scales = false;
        public String eyeColor = "blue";
        public int numOfTeeth = 26;
        public Integer numOfLegs = 4;
        public Double weight = 212.07;
        public double weight2 = 160.12;
    }
    @Entity
    class Car {
        @Id
        public int carId = 56789;
        public String paintColor = "silver";
        public Double weight = 4128.13;
        public int horsePower = 357;
    }

    @BeforeAll
    public void init() {
        Animal animal = new Animal();
        Car car = new Car();
        models2.add(animal);
        models2.add(car);
    }

    @Test
    public void testCreateTable() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        AnnotationStrategy aS = new AnnotationStrategy();
        Class animal = Animal.class;
        String result = aS.createTable(animal);

        assertEquals("CREATE TABLE Animals (\n" +
                "  animalId serial,\n" +
                "  fur BOOL,\n" +
                "  scales BOOL,\n" +
                "  eyeColor VARCHAR(50),\n" +
                "  numOfTeeth INTEGER,\n" +
                "  numOfLegs INTEGER,\n" +
                "  weight DECIMAL(10,2),\n" +
                "  weight2 DECIMAL(10,2),\n" +
                "  primary key (animalId)\n" +
                ");", result);
    }
    @Test
    public void testInsert() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        AnnotationStrategy aS = new AnnotationStrategy();
        Animal animal = new Animal();
        String result = aS.insert(animal);

        assertEquals("INSERT INTO Animals VALUES (default, true, false, 'blue', 26, 4, 212.07, 160.12);", result);
    }
    @Test
    public void testUpdate() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        AnnotationStrategy aS = new AnnotationStrategy();
        Animal animal = new Animal();
        String result = aS.update(animal);

        assertEquals("UPDATE Animals SET fur = true, scales = false, eyeColor = 'blue', numOfTeeth = 26," +
                " numOfLegs = 4, weight = 212.07, weight2 = 160.12 WHERE animalId = 12345;", result );
    }
    @Test
    public void testGetAll() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        AnnotationStrategy aS = new AnnotationStrategy();
        Class animal = Animal.class;
        String result = aS.getAll(animal);

        assertEquals("SELECT * FROM Animals;", result);
    }
    @Test
    public void testGet() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        AnnotationStrategy aS = new AnnotationStrategy();
        Class animal = Animal.class;
        String result = aS.get(animal, 12345);

        assertEquals("SELECT * FROM Animals WHERE animalId = 12345;", result);
    }
    @Test
    public void testDelete() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        AnnotationStrategy aS = new AnnotationStrategy();
        Class animal = Animal.class;
        String result = aS.delete(animal, 12345);

        assertEquals("DELETE FROM Animals WHERE animalId = 12345;", result);
    }
    @Test
    public void testBuildSchema() {
        AnnotationStrategy aS = new AnnotationStrategy();
        String result = aS.buildSchema(models2);

        assertEquals("CREATE TABLE Animals (\n" +
                "  animalId serial,\n" +
                "  fur BOOL,\n" +
                "  scales BOOL,\n" +
                "  eyeColor VARCHAR(50),\n" +
                "  numOfTeeth INTEGER,\n" +
                "  numOfLegs INTEGER,\n" +
                "  weight DECIMAL(10,2),\n" +
                "  weight2 DECIMAL(10,2),\n" +
                "  primary key (animalId)\n" +
                ");CREATE TABLE Car (\n" +
                "  carId serial,\n" +
                "  paintColor VARCHAR(50),\n" +
                "  weight DECIMAL(10,2),\n" +
                "  horsePower INTEGER,\n" +
                "  primary key (carId)\n" +
                ");", result);
    }
    @Test
    public void testAddModel(){
        AnnotationStrategy aS = new AnnotationStrategy();
        Animal animal = new Animal();
        Car car = new Car();

        assertEquals(0, models1.size());
        models1.add(aS.addModel(animal));
        models1.add(aS.addModel(car));
        assertEquals(2, models1.size());
    }

    @AfterAll
    public void cleanUp(){
        models1.clear();
    }
}
