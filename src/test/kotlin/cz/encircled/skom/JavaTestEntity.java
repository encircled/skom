package cz.encircled.skom;

public class JavaTestEntity implements Convertable {

    private String name;

    private String another;

    private Boolean isBoolean;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnother() {
        return another;
    }

    public void setAnother(String another) {
        this.another = another;
    }

    public Boolean getBoolean() {
        return isBoolean;
    }

    public void setBoolean(Boolean aBoolean) {
        isBoolean = aBoolean;
    }

}
