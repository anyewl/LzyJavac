package work.liziyun;



class Anlimal{
    public String name = "23";
}

public class Demo03 extends Anlimal  {

    public Anlimal anlimal = new Anlimal();

    public static String getName(){
        return "23";
    }


    public static void main(String []args){
        Demo03 demo03 = new Demo03();
        System.out.println( demo03.anlimal.name );
    }
}
