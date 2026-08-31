package std;

public class sleep {
    
    // the unit of t is milliseconds
    // jl_sleep(1000) --> sleeps for 1 second
    public static void jl_sleep(int t){
        try{
            //System.out.println("Jiaqi is testing Sleep...");
            Thread.sleep(t);
            //System.out.println("Awake!");
        } catch(InterruptedException e){
            System.err.println("Sleep was interrupted");
        }

	}
   
}