package util;

import annotations.Entity;
import annotations.Id;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AnnotationStrategyTest {
    @Test
    public void testCreateTable(){
        @Entity(name = "ClassName")
        class Animal {
            @Id
            public boolean animalId;
            public boolean fur = true;
            public Boolean scales = false;
            public String eyeColor = "blue";
            public int numOfTeeth = 26;
            public Integer numOfLegs = 4;
            public Double weight = 212.07;
            public double weight2 = 160.12;
        }
        AnnotationStrategy aS = new AnnotationStrategy();
        Class animal = Animal.class;
        String result = aS.createTable(animal);

        assertEquals("CREATE TABLE ClassName (\n" +
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
}
