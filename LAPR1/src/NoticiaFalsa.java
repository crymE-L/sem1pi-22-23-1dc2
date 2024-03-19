import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class NoticiaFalsa {

    static final int LIMIT = 300;
    static Scanner ler = new Scanner(System.in);

    public static void main(String[] args) throws IOException, InterruptedException {

        double populacao, passo;
        int numeroDeDias, algoritmo, numeroDeLinhas, linhaDoNome = 0;
        String nomeDoFicheiroSIR, nomeDaPessoaAnalisada;

        if (args.length == 0) {

            // Modo Interativo

            System.out.println("Escreva o nome do ficheiro SIR (ex.: ficheiroSIR.csv):");
            nomeDoFicheiroSIR = ler.next();

            final String FILE = nomeDoFicheiroSIR;
            File file = new File(FILE);

            numeroDeLinhas = contarNumeroDeLinhas(file);

            do {
                System.out.println("Escreva o nome da pessoa que deseja analisar:");
                nomeDaPessoaAnalisada = ler.next();

                // Criamos este ciclo para procurar pelo nome da pessoa que o utilizador deseja analisar
                for ( int linha = 2; linha <= numeroDeLinhas; linha++) {
                    linhaDoNome = procurarPeloNome(file, nomeDaPessoaAnalisada, linha);

                    if(linhaDoNome != -1){
                        break;
                    }
                }

                if(linhaDoNome == -1){
                    System.out.println("O nome que colocou é inválido.");

                }

            }while ( linhaDoNome == -1);

            do {
                System.out.println("Escreva abaixo o algoritmo que deseja utilizar, utilizando um número (Euler -> 1, Runge-Kutta -> 2):");
                algoritmo = ler.nextInt();

            } while (algoritmo != 1 && algoritmo != 2);

            System.out.println("Escreva abaixo o tamanho da população:");
            populacao = ler.nextDouble();

            System.out.println("Escreva abaixo o número de dias para a previsão do modelo:");
            numeroDeDias = ler.nextInt();

            do {
                System.out.println("Escreva abaixo o passo h (0 < h <= 1):");
                passo = ler.nextDouble();

            } while (passo <= 0 || passo > 1);

            fazerAnalise(nomeDoFicheiroSIR, populacao, algoritmo, passo, numeroDeDias, linhaDoNome);

        } else {

            // Modo Não Interativo

            nomeDoFicheiroSIR = args[0];

            final String FILE = nomeDoFicheiroSIR;
            File file = new File(FILE);

            numeroDeLinhas = contarNumeroDeLinhas(file);

            algoritmo = Integer.parseInt(args[2]);
            passo = Double.parseDouble(args[4]);
            populacao = Double.parseDouble(args[6]);
            numeroDeDias = Integer.parseInt(args[8]);

            // Criamos este ciclo para fazer a análise de todas as pessoas presentes no ficheioSIR
            for (int linha = 2; linha <= numeroDeLinhas; linha++) {
                fazerAnalise(nomeDoFicheiroSIR, populacao, algoritmo, passo, numeroDeDias, linha);

            }

        }

    }


    // Neste módulo procuramos o nome da pessoa que o utilizador deseja analisar (modo interativo)
    public static int procurarPeloNome(File file, String nomeDaPessoaAnalisada, int linha) throws FileNotFoundException {

        String[] arrElementosDeCadaLinha = retirarElementosDoFicheiro(file, linha, 5);

        String lowercaseName = arrElementosDeCadaLinha[0].toLowerCase();
        String lowercaseIntendedName = nomeDaPessoaAnalisada.toLowerCase();

        if(lowercaseName.equals(lowercaseIntendedName)) {
            return linha;

        }

        return -1;
    }

    // Aqui é feita a análise, sendo depois gerado tanto o gráfico PNG como o ficheiro CSV
    public static void fazerAnalise(String nomeDoFicheiroSIR, double populacao, int algoritmo, double passo, int numeroDeDias, int linha) throws IOException, InterruptedException {
        String nomeDaPessoaAnalisada;
        double beta, gama, ro, alfa;

        String[] arrElementos = leituraDosElementos(nomeDoFicheiroSIR, linha);


        /*
        Como todos os elementos do ficheiro vêm como String e com uma vírgula em vez de um ponto, com estes comandos
        conseguimos fazer a transformação que desejamos, passando os elementos que queremos em double para double e aqueles
        que trouxerem uma vírgula substituir por um ponto
         */
        beta = Double.parseDouble(arrElementos[0].replace(",", "."));
        gama = Double.parseDouble(arrElementos[1].replace(",", "."));
        ro = Double.parseDouble(arrElementos[2].replace(",", "."));
        alfa = Double.parseDouble(arrElementos[3].replace(",", "."));
        nomeDaPessoaAnalisada = arrElementos[4];

        String nomeDoFicheiroDeSaida = fazerNomeDoFicheiroDeSaida(nomeDaPessoaAnalisada, algoritmo, passo, populacao, numeroDeDias);

        double sInicial = populacao - 1;
        double iInicial = 1;
        double rInicial = 0;

        double[][] arrDados;

        if (algoritmo == 1) {

            arrDados = fazerEuler(numeroDeDias, sInicial, iInicial, rInicial, passo, beta, gama, ro, alfa);

            arrDados[0][0] = sInicial;
            arrDados[1][0] = iInicial;
            arrDados[2][0] = rInicial;

            double[] arrPopulacao = calcularPopulacao(arrDados, numeroDeDias);

            criarFicheiroCSV(arrDados, arrPopulacao, numeroDeDias, nomeDoFicheiroDeSaida);

        } else if (algoritmo == 2) {

            arrDados = fazerRk4(numeroDeDias, sInicial, iInicial, rInicial, passo, beta, gama, ro, alfa);

            arrDados[0][0] = sInicial;
            arrDados[1][0] = iInicial;
            arrDados[2][0] = rInicial;

            double[] arrPopulacao = calcularPopulacao(arrDados, numeroDeDias);

            criarFicheiroCSV(arrDados, arrPopulacao, numeroDeDias, nomeDoFicheiroDeSaida);

        }

        criarGraficoPNG(algoritmo, nomeDoFicheiroDeSaida);
        Runtime.getRuntime().exec("gnuplot gnuplot.txt");

        /*
        Como no caso do modo não interativo é criado mais do que um ficheiro e um gráfico, utilizamos o comando "Thread.sleep()", para conseguirmso ter
        tempo para ser gerado cada um dos gráficos (pertencentes a cada uma das pessoas do ficheiro SIR)
         */
        Thread.sleep(100);

    }

    // Agora criámos este módulo para poder-mos ler e retirar os elementos que precisamos do ficheiro SIR
    public static String[] leituraDosElementos(String nomeDoFicheiroSIR, int linha) throws FileNotFoundException {
        String[] arrElementos = new String[5];

        final String FILE = nomeDoFicheiroSIR;

        File file = new File(FILE);

        String[] arrFicheiro = retirarElementosDoFicheiro(file, linha, 5);

        arrElementos[0] = arrFicheiro[1];
        arrElementos[1] = arrFicheiro[2];
        arrElementos[2] = arrFicheiro[3];
        arrElementos[3] = arrFicheiro[4];
        arrElementos[4] = arrFicheiro[0];

        return arrElementos;
    }

    // Para nos ajudar, no futuro, criámos este módulo para retirar os elementos do ficheiro SIR
    public static String[] retirarElementosDoFicheiro(File file, int linha, int numeroDeElementos) throws FileNotFoundException {
        Scanner ler = new Scanner(file);
        String[] arrElementos = new String[numeroDeElementos];
        int contador = 0;

        String posicaoLinha = encontrarLinha(file, linha);

        if (!posicaoLinha.equals("")) {

            /*
            Como o ficheiro que irá ser lido é um ficheiro CSV e sabemos que estes ficheiros são divididos por ";", com este ciclo "for" vamos
            retirar os elementos qu nos interessam, fazendo essa divisão cada vez que é encontrado um ";"
             */
            for (String elemento : posicaoLinha.split(";")) {
                arrElementos[contador] = elemento;
                contador++;
            }
        }

        ler.close();

        return arrElementos;
    }

    // Criámos esta função para encontar-nos uma linha no ficheiro
    public static String encontrarLinha(File file, int linha) throws FileNotFoundException {
        Scanner ler = new Scanner(file);
        String[] arrLinhas = new String[LIMIT];
        int contador = 0;

        do {
            arrLinhas[contador] = ler.nextLine();
            contador++;

        } while (ler.hasNextLine());

        return arrLinhas[linha - 1];
    }

    // Este módulo conta o número de linhas, algo que nos vai ser útil para analisar as pessoas presentes no ficheiro SIR para serem analisadas
    public static int  contarNumeroDeLinhas(File file)throws FileNotFoundException {
        Scanner ler = new Scanner(file);
        int contador = 0;

        while(ler.hasNextLine() && !ler.nextLine().equals("")){
            contador++;

        }
        ler.close();

        return contador;
    }

    // Criámos este módulo com o intuito de utlizar o método de Euler para realizar a análise se essa for a opção escolhida pelo utilizador
    public static double[][] fazerEuler(int numeroDeDias, double si, double ii, double ri, double h, double beta, double gama, double ro, double alfa){
        double[][] arrDados = new double[3][numeroDeDias];

        for (int i = 1; i < numeroDeDias; i++) {
            int passosPorDia = numeroDePassos(i, h);

            double[] arrDadosDeCadaDia = fazerEulerDeCadaDia(passosPorDia, si, ii, ri, h, beta, gama, ro, alfa);

            arrDados[0][i] = arrDadosDeCadaDia[0];
            arrDados[1][i] = arrDadosDeCadaDia[1];
            arrDados[2][i] = arrDadosDeCadaDia[2];

        }

        return arrDados;
    }


    public static double[] fazerEulerDeCadaDia(int passosPorDia, double sInicial, double iInicial, double rInicial, double h, double beta, double gama, double ro, double alfa){
        double[] arrDados = new double[3];
        double sFinal = 0, iFinal = 0, rFinal = 0;
        arrDados[0] = sFinal;
        arrDados[1] = iFinal;
        arrDados[2] = rFinal;

        for (int i = 0; i < passosPorDia; i++) {
            sFinal = sInicial + h * (-(beta * sInicial * iInicial));
            iFinal = iInicial + h * ((ro * beta * sInicial * iInicial) - (gama * iInicial) + (alfa * rInicial));
            rFinal = rInicial + h * ((gama * iInicial) - (alfa * rInicial) + ((1 - ro) * beta * sInicial * iInicial));

            sInicial = sFinal;
            iInicial = iFinal;
            rInicial = rFinal;

        }
        arrDados[0] = sFinal;
        arrDados[1] = iFinal;
        arrDados[2] = rFinal;

        return arrDados;
    }


    // Neste módulo é feito o método de Runge-Kutta para realizar a análise, opção que o utilizador também vai poder escolher
    public static double[][] fazerRk4(int numeroDeDias, double si, double ii, double ri, double h, double beta, double gama, double ro, double alfa){
        double[][] arrDados = new double[3][numeroDeDias];

        for (int i = 1; i < numeroDeDias; i++) {
            int passosPorDia = numeroDePassos(i, h);

            double[] arrDadosDeCadaDia = fazerRk4DeCadaDia(passosPorDia, si, ii, ri, h, beta, gama, ro, alfa);

            arrDados[0][i] = arrDadosDeCadaDia[0];
            arrDados[1][i] = arrDadosDeCadaDia[1];
            arrDados[2][i] = arrDadosDeCadaDia[2];
        }

        return arrDados;
    }

    public static double[] fazerRk4DeCadaDia(int passosPorDia, double sInicial, double iInicial, double rInicial, double h, double beta, double gama, double ro, double alfa){
        double[] arrDados = new double[3];

        double sFinal = 0, iFinal = 0, rFinal = 0;

        arrDados[0] = sFinal;
        arrDados[1] = iFinal;
        arrDados[2] = rFinal;

        for (int i = 0; i < passosPorDia; i++) {
            double k1s = h * (-beta * sInicial * iInicial);
            double k1i = h * ((ro * beta * sInicial * iInicial) - (gama * iInicial) + (alfa * rInicial));
            double k1r = h * ((gama * iInicial) - (alfa * rInicial) + ((1 - ro) * beta * sInicial * iInicial));

            double k2s = h * (-beta * (sInicial + (k1s / 2)) * (iInicial + (k1i / 2)));
            double k2i = h * ((ro * beta * (sInicial + (k1s / 2)) * (iInicial + (k1i / 2))) - (gama * (iInicial + (k1i / 2))) + (alfa * (rInicial + (k1r / 2))));
            double k2r = h * ((gama * (iInicial + (k1i / 2))) - (alfa * (rInicial + (k1r / 2))) + ((1 - ro) * beta * (sInicial + (k1s / 2)) * (iInicial + (k1i / 2))));

            double k3s = h * (-beta * (sInicial + (k2s / 2)) * (iInicial + (k2i / 2)));
            double k3i = h * ((ro * beta * (sInicial + (k2s / 2)) * (iInicial + (k2i / 2))) - (gama * (iInicial + (k2i / 2))) + (alfa * (rInicial + (k2r / 2))));
            double k3r = h * ((gama * (iInicial + (k2i / 2))) - (alfa * (rInicial + (k2r / 2))) + ((1 - ro) * beta * (sInicial + (k2s / 2)) * (iInicial + (k2i / 2))));

            double k4s = h * (-beta * (sInicial + k3s) * (iInicial + k3i));
            double k4i = h * ((ro * beta * (sInicial + k3s) * (iInicial + k3i)) - (gama * (iInicial + k3i)) + (alfa * (rInicial + k3r)));
            double k4r = h * ((gama * (iInicial + k3i)) - (alfa * (rInicial + k3r)) + ((1 - ro) * beta * (sInicial + k3s) * (iInicial + k3i)));

            double ks = ((k1s + (k2s * 2) + (k3s * 2) + k4s) / 6);
            double ki = ((k1i + (k2i * 2) + (k3i * 2) + k4i) / 6);
            double kr = ((k1r + (k2r * 2) + (k3r * 2) + k4r) / 6);

            sFinal = sInicial + ks;
            iFinal = iInicial + ki;
            rFinal = rInicial + kr;

            sInicial = sFinal;
            iInicial = iFinal;
            rInicial = rFinal;

        }

        arrDados[0] = sFinal;
        arrDados[1] = iFinal;
        arrDados[2] = rFinal;

        return arrDados;
    }

    // Este módulo é muito útil para a realização de ambos os métodos (Euler e Runge-Kutta)
    private static int numeroDePassos(int dia, double passo) {
        return (int) (dia/passo);
    }


    // Neste módulo é calculada a população total, algo que nos é pedido que esteja presente no ficheiro CSV
    public static double[] calcularPopulacao(double[][] arrDados, int numeroDeDias) {
        double arrPopulacao[] = new double[numeroDeDias];

        for (int i = 0; i < numeroDeDias; i++) {
            arrPopulacao[i] = arrDados[0][i] + arrDados[1][i] + arrDados[2][i];
        }

        return arrPopulacao;
    }

    // No módulo que se segue é criado o nome dos ficheiros de saída (PNG e CSV)
    public static String fazerNomeDoFicheiroDeSaida(String nome, int algoritmo, double passo, double populacao, int numeroDeDias){
        String nomeDoficheiroDeSaida, passoString, algoritmoString, populacaoString, numeroDeDiasString;

        passoString = String.valueOf(passo);
        populacaoString = String.valueOf(populacao);
        numeroDeDiasString = String.valueOf(numeroDeDias);
        algoritmoString = String.valueOf(algoritmo);

        nomeDoficheiroDeSaida = nome + "m" + algoritmoString + "p" + passoString + "t" + populacaoString + "d" + numeroDeDiasString;

        return nomeDoficheiroDeSaida;
    }

    // A seguir criámos o módulo que irá criar o ficheiro .CSV
    public static void criarFicheiroCSV(double[][] arrDados, double[] arrPopulacao, int numeroDeDias, String nomeDoFicheiroDeSaida) throws FileNotFoundException {
        nomeDoFicheiroDeSaida = nomeDoFicheiroDeSaida + ".csv";
        PrintWriter pr = new PrintWriter(nomeDoFicheiroDeSaida);
        pr.println("Dias; Suscetíveis; Infetados; Recuperados; População");

        for (int i = 0; i < numeroDeDias; i++) {
            pr.print(i);
            pr.print(";");
            pr.print(arrDados[0][i]);
            pr.print(";");
            pr.print(arrDados[1][i]);
            pr.print(";");
            pr.print(arrDados[2][i]);
            pr.print(";");
            pr.print(arrPopulacao[i]);
            pr.print(";");
            pr.println();
        }

        pr.close();
    }

    // Neste módulo é criado o gráfico PNG com o auxílio do gnuplot
    public static void criarGraficoPNG(int algoritmo, String nomeDoFicheiroDeSaida) throws FileNotFoundException {
        PrintWriter pr = new PrintWriter("gnuplot.txt");

        if (algoritmo == 1) {

            pr.println("set terminal pngcairo size 1280,800");
            pr.println("set output '" + nomeDoFicheiroDeSaida + ".png'");
            pr.println("set ylabel 'População'");
            pr.println("set xlabel 'Número de Dias'");
            pr.println("set title ' Método de Euler '");
            pr.println("set datafile separator ';' ");
            pr.println("plot '"+ nomeDoFicheiroDeSaida +".csv' using 1:2 w l title 'S' , '"+ nomeDoFicheiroDeSaida +".csv' using 1:3 w l title 'I', '"+ nomeDoFicheiroDeSaida +".csv' using 1:4 w l title 'R'");

        } else {
            pr.println("set terminal pngcairo size 1280,800");
            pr.println("set output '" + nomeDoFicheiroDeSaida + ".png'");
            pr.println("set ylabel 'População'");
            pr.println("set xlabel 'Número de Dias'");
            pr.println("set title ' Método de Runge-Kutta '");
            pr.println("set datafile separator ';' ");
            pr.println("plot '"+ nomeDoFicheiroDeSaida +".csv' using 1:2 w l title 'S' , '"+ nomeDoFicheiroDeSaida +".csv' using 1:3 w l title 'I', '"+ nomeDoFicheiroDeSaida +".csv' using 1:4 w l title 'R'");

        }

        pr.close();
    }

}