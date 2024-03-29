import java.util.ArrayList;
import java.util.Random;
import java.util.Iterator;

class Player {

    // - - - - - - VARIABLES - - - - - -

    double best_prob;
    HMM3 model;

    ArrayList<HMM3>[] HMM_list;


    // - - - - - - PLAYER FUNCTION - - - - - -

    public Player() {

        HMM_list = new ArrayList[Constants.COUNT_SPECIES];
        HMM3[] models = new HMM3[Constants.COUNT_SPECIES];
        for (int i = 0; i < Constants.COUNT_SPECIES; i++){
            models[i] = new HMM3(Constants.COUNT_SPECIES, Constants.COUNT_MOVE, -1, null);
            HMM_list[i] = new ArrayList();
            HMM_list[i].add(models[i]);
        }
    }

    /**
     * Shoot!
     *
     * This is the function where you start your work.
     *
     * You will receive a variable pState, which contains information about all
     * birds, both dead and alive. Each bird contains all past moves.
     *
     * The state also contains the scores for all players and the number of
     * time steps elapsed since the last time this function was called.
     *
     * @param pState the GameState object with observations etc
     * @param pDue time before which we must have returned
     * @return the prediction of a bird we want to shoot at, or cDontShoot to pass
     */
    public Action shoot(GameState pState, Deadline pDue) {

        //int bestMove, bestBird, bestIdx;
        //double highest_prob, bestGuessProb, maxSum;

        if (pState.getBird(0).getSeqLength() >= (100 - pState.getNumBirds())){

            int bestBird = -1;
            int bestIdx = -1;
            int bestMove = -1;
            double maxSum = -1;
            double highest_prob = -1;
            double bestGuessProb = -1;

            //Iterate through the birds
            for (int i = 0; i < pState.getNumBirds(); i++) {
                Bird bird = pState.getBird(i);

                int T = 0;
                int N = 6;
                int M = 9;

                if (bird.isDead()){
                    continue;
                }

                //Stop checking bird if dead
                for (int j = 0; j < bird.getSeqLength(); j++) {
                    if (!bird.wasDead(j))
                        T += 1;
                }

                //Creating the observation sequence of every bird
                int[] obs_sequence = new int[T];

                for (int k = 0; k < T; k++) {
                    obs_sequence[k] = bird.getObservation(k);
                }

                HMM3 model = new HMM3(N, M, T, obs_sequence);

                model.HMM_algorithm();
                model.validate();

                int[][] temp_matrix = new int[1][1];
                temp_matrix[0][0] = bird.getLastObservation();

                int idxBird = classifyBird(temp_matrix[0]);

                double probability = model.ShotProb();

                //Probability = next move, best_prob = current bird
                double sum = 0.8*probability + 0.2*best_prob;

                if (sum > maxSum){
                    highest_prob = probability;
                    bestBird = i;
                    bestIdx = idxBird;
                    bestMove = model.ShootBird();
                    bestGuessProb = best_prob;
                    maxSum = sum;
                }
            }

            //Shooting only if probability is high and it's not a black stork
            if (maxSum > 0.7 && bestIdx != 5){
                return new Action(bestBird, bestMove);
            }

        }

        // This line chooses not to shoot.
        return cDontShoot;

        // This line would predict that bird 0 will move right and shoot at it.
        // return Action(0, MOVE_RIGHT);
    }

    /**
     * Guess the species!
     * This function will be called at the end of each round, to give you
     * a chance to identify the species of the birds for extra points.
     *
     * Fill the vector with guesses for the all birds.
     * Use SPECIES_UNKNOWN to avoid guessing.
     *
     * @param pState the GameState object with observations etc
     * @param pDue time before which we must have returned
     * @return a vector with guesses for all the birds
     */
    public int[] guess(GameState pState, Deadline pDue) {

        //This is the list of guesses that we'll make
        int[] lGuess = new int[pState.getNumBirds()];

        //Calculating actual length of observations for each bird
        for (int i = 0; i < pState.getNumBirds(); i++) {
            Bird bird = pState.getBird(i);
            int T = 0;

            //Stop checking bird if dead
            for (int j = 0; j < bird.getSeqLength(); j++) {
                if (!bird.wasDead(j))
                    T += 1;
            }

            //Creating the observation sequence of every bird
            int[] obs_sequence = new int[T];

            for (int k = 0; k < T; k++) {
                obs_sequence[k] = bird.getObservation(k);
            }

            //Guessing the bird species
            lGuess[i] = classifyBird(obs_sequence);

        }

        return lGuess;
    }

    public int classifyBird(int[] obs_input){
        int bestIdx;
        double prob, max_prob;

        Random random = new Random();
        bestIdx = random.nextInt(Constants.COUNT_SPECIES-1);
        max_prob = 0.0;

        Iterator<HMM3> iter;
        for (int i = 0; i < HMM_list.length; i++){
            iter = HMM_list[i].iterator();

            while(iter.hasNext()){
                HMM3 placeholder = iter.next();
                prob = placeholder.alphaProb(obs_input);
                if(prob > max_prob){
                    max_prob = prob;
                    bestIdx = i;
                    this.model = placeholder;
                }
            }
        }

        best_prob = max_prob;
        return bestIdx;
    }

    /**
     * If you hit the bird you were trying to shoot, you will be notified
     * through this function.
     *
     * @param pState the GameState object with observations etc
     * @param pBird the bird you hit
     * @param pDue time before which we must have returned
     */
    public void hit(GameState pState, int pBird, Deadline pDue) {
        System.err.println("HIT BIRD!!!");
    }

    /**
     * If you made any guesses, you will find out the true species of those
     * birds through this function.
     *
     * @param pState the GameState object with observations etc
     * @param pSpecies the vector with species
     * @param pDue time before which we must have returned
     */
    public void reveal(GameState pState, int[] pSpecies, Deadline pDue) {

        //Calculating actual length of observations for each bird
        for (int i = 0; i < pSpecies.length; i++){
            if(pSpecies[i] != -1){          //Make sure it's not unknown
                Bird bird = pState.getBird(i);
                int T = 0;

                //Stop checking bird if dead
                for (int j = 0; j < bird.getSeqLength(); j++) {
                    if (!bird.wasDead(j))
                        T += 1;
                }

                int[] obs_sequence = new int[T];

                for (int k = 0; k < T; k++) {
                    obs_sequence[k] = bird.getObservation(k);
                }

                //Calculate the HMM model
                HMM3 model = new HMM3(Constants.COUNT_SPECIES, Constants.COUNT_MOVE, -1, null);
                model.updateObsSeq(T, obs_sequence);
                model.HMM_algorithm();

                HMM_list[pSpecies[i]].add(model);

            }
        }
    }

    public static final Action cDontShoot = new Action(-1, -1);
}
