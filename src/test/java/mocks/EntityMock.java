package mocks;

public class EntityMock {

    private int id;
    private String name;

    public EntityMock(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
