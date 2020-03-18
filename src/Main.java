
import java.time.ZonedDateTime;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.cmd.Vehicle;
import it.polito.appeal.traci.SumoTraciConnection;

public class Main {

    public static void main(String[] args) {
        String simulationStartTime = String.valueOf(ZonedDateTime.now().toInstant().toEpochMilli());
        String currentTramsGap = String.valueOf(0);

        Double currentVehicleDelay, currentPos;
        double[] mylist = new double[300];
        double[] pos = new double[7];

        TreeMap<Integer, Double> vehiclesWaitingTimes = new TreeMap<Integer, Double>();

        Arrays.fill(mylist, 0.0);
        Arrays.fill(pos, 0);

        float somme = 0;

        String sumo_bin = "sumo-gui.exe";
        String config_file = "D:\\desktop\\ILISI2\\Projet Integration POO\\SumoProject\\src\\traasTest\\data\\config.sumocfg";
        double step_length = 0.1;

        boolean startDataGathering = false;

        try {
            SumoTraciConnection conn = new SumoTraciConnection(sumo_bin, config_file);
            conn.addOption("step-length", step_length + "");
            conn.addOption("start", "true"); // start sumo immediately

            // start Traci Server
            conn.runServer();
            conn.setOrder(1);

            for (int i = 0; i < 3600; i++) {

                double timeSeconds = (double) conn.do_job_get(Simulation.getTime());

                if (timeSeconds == 160)
                    break;

                conn.do_timestep();
                conn.do_job_set(Vehicle.addFull("v" + i, "r1", "car", "now", "0", "0", "max", "current", "max",
                        "current", "", "", "", 0, 0));

                @SuppressWarnings("unchecked")
                List<String> supplierNames1 = (List<String>) conn.do_job_get(Vehicle.getIDList());


                for (String vehicleId : supplierNames1) {

                    if ((conn.do_job_get(Vehicle.getTypeID(vehicleId)).equals("vType_0"))) {
                        currentPos = Double
                                .parseDouble(conn.do_job_get(Vehicle.getPosition(vehicleId)).toString().split(",")[0]);
                        if (currentPos > 460.0) {
                            conn.do_job_set(Trafficlight.setRedYellowGreenState("gneJ1", "rrrrGGG"));
                            //System.out.println("RED LIGHT");
                            startDataGathering = true;
                        }
                        if (currentPos > 540) {
                            conn.do_job_set(Trafficlight.setRedYellowGreenState("gneJ1", "GGGGrrr"));
                            //System.out.println("GREEN LIGHT");
                            if (startDataGathering == true)
                                startDataGathering = false;
                        }
                    }


                    if ((conn.do_job_get(Vehicle.getTypeID(vehicleId)).equals("car"))) {

                        int ind = Integer.parseInt(vehicleId.substring(1));
                        vehiclesWaitingTimes.put((Integer) ind, (Double) conn.do_job_get(Vehicle.getAccumulatedWaitingTime(vehicleId)));
                        currentVehicleDelay = (double) conn.do_job_get(Vehicle.getWaitingTime(vehicleId));

                        Random random = new Random();
                        conn.do_job_set(Vehicle.setSpeed(vehicleId, 100 + random.nextDouble() % 100));
                        if (currentVehicleDelay > mylist[ind])
                            mylist[ind] = currentVehicleDelay;
                    }
                }
            }

            Gson gson = new GsonBuilder().create();
            String data = gson.toJson(vehiclesWaitingTimes);
            data = "{ \"startTime\":" + simulationStartTime + ", \"gap\":" + currentTramsGap + ", \"data\":" + data + "}";
            KafkaMsgProducer.runProducer(data);

            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}


