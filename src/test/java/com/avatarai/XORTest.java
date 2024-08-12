package com.avatarai;

public class XORTest {
    public static void main(String[] args)
    {

        Avatar net = new Avatar(2, 1, 5, 1);

        double[][] inputs = new double[4][2];
        double[][] outputs = new double[4][1];

        inputs[0][0] = -1.0;
        inputs[0][1] = -1.0;
        outputs[0][0] = 0.0;
        inputs[1][0] = 1.0;
        inputs[1][1] = -1.0;
        outputs[1][0] = 1.0;
        inputs[2][0] = -1.0;
        inputs[2][1] = 1.0;
        outputs[2][0] = 1.0;
        inputs[3][0] = 1.0;
        inputs[3][1] = 1.0;
        outputs[3][0] = 0.0;

        for (int rep=0; rep<1000; rep++)
        {
            double netError = 0.0;
            for (int j=0; j<4; j++) // For each test set
            {
                double[] result = net.train(inputs[j], outputs[j], 5, 0.01);
                double error = 0.0;
                for (int i=0; i<result.length; i++)
                {
                    error += Math.pow(outputs[j][i] - result[i], 2);
                }
                netError += Math.sqrt(error);
            }
            System.out.println(rep + ", " + netError/4.0);
        }

        System.out.println("=========================================================");
        System.out.print(net);
        System.out.println("=========================================================");
        // Now recheck to ensure that the learning has "stuck"
        for (int j=0; j<4; j++)
        {
            double[] result= net.present(inputs[j]);
            double output1 = Math.round(result[0]*100.0)/100.0;
            System.out.println(outputs[j][0] + " : " + output1);
        }

    }

}
