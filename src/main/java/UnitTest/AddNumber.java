package UnitTest;

public class AddNumber {
    private Integer number1;
    private Integer number2;

    public AddNumber(Integer number1, Integer number2) {
        this.number1 = number1;
        this.number2 = number2;
    }

    public Integer add(){
        return number1 + number2;
    }
}
