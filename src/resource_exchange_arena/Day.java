package resource_exchange_arena;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class Day {
    // List of all the possible allocations that exist in the current simulation.
    private final List<Integer> availableTimeSlots = new ArrayList<>();

    /**
     * Each Simulation run consists of a number of days, each day consists of requesting and being allocated time slots,
     * exchanging those slots with other agents, and agents using social learning to learn from their experiences.
     *
     * @param daysOfInterest Integer array containing the days be shown in graphs produced after the simulation.
     * @param additionalData Boolean value that configures the simulation to output the state of each agent after each
     *                       exchange and at the end of each day.
     * @param day Integer value representing the current day being simulated.
     * @param exchanges Integer value representing the number of times all agents perform pairwise exchanges per day.
     * @param maximumPeakConsumption Integer value representing how many agents can be allocated to each time slot.
     * @param uniqueTimeSlots Integer value representing the number of unique time slots available in the simulation.
     * @param slotsPerAgent Integer value representing the number of time slots each agent requires.
     * @param numberOfAgentsToEvolve Integer value representing the number of Agents who's strategy will change at the
     *                               end of each day.
     * @param uniqueAgentTypes Integer ArrayList containing each unique agent type that exists when the simulation
     *                         begins.
     * @param agents Array List of all the agents that exist in the current simulation.
     * @param endOfDaySatisfactions Stores the satisfaction of each agent at the end of days of interest.
     * @param endOfRoundAverageSatisfactions Stores the average satisfaction for each agent type at the end of each
     *                                       round.
     * @param endOfDayAverageSatisfactions Stores the average satisfaction for each agent type at the end of each day.
     * @param endOfDayPopulationDistributions Stores the population of each agent type at the end of each day.
     * @param averageCSVWriter Writes additional data on the average satisfaction of every agent at the end of each day
     *                         when additional data is requested.
     * @param individualCSVWriter Writes additional data on the individual agents satisfaction after each exchange when
     *                            additional data is requested.
     * @exception IOException On input error.
     * @see IOException
     */
    Day(
            int[] daysOfInterest,
            boolean additionalData,
            int day,
            int exchanges,
            int maximumPeakConsumption,
            int uniqueTimeSlots,
            int slotsPerAgent,
            int numberOfAgentsToEvolve,
            ArrayList<Integer> uniqueAgentTypes,
            ArrayList<Agent> agents,
            ArrayList<ArrayList<Double>> endOfDaySatisfactions,
            ArrayList<ArrayList<Double>> endOfRoundAverageSatisfactions,
            ArrayList<ArrayList<Double>> endOfDayAverageSatisfactions,
            ArrayList<ArrayList<ArrayList<Integer>>> endOfDayPopulationDistributions,
            FileWriter averageCSVWriter,
            FileWriter individualCSVWriter
    ) throws IOException{

        // Fill the available time slots with all the slots that exist each day.
        for (int timeSlot = 1; timeSlot <= uniqueTimeSlots; timeSlot++) {
            for (int unit = 1; unit <= maximumPeakConsumption; unit++) {
                availableTimeSlots.add(timeSlot);
            }
        }

        // Agents start the day by requesting and receiving an allocation of time slots.
        Collections.shuffle(agents, ResourceExchangeArena.random);
        for (Agent a : agents) {
            ArrayList<Integer> requestedTimeSlots = a.requestTimeSlots(uniqueTimeSlots);
            ArrayList<Integer> allocatedTimeSlots = getRandomInitialAllocation(requestedTimeSlots);
            a.receiveAllocatedTimeSlots(allocatedTimeSlots);
        }

        // The random and optimum average satisfaction scores are calculated before exchanges take place.
        double randomAllocations = CalculateSatisfaction.averageAgentSatisfaction(agents);
        double optimumAllocations = CalculateSatisfaction.optimumAgentSatisfaction(agents);

        if (additionalData) {
            averageCSVWriter.append(String.valueOf(ResourceExchangeArena.seed));
            averageCSVWriter.append(",");
            averageCSVWriter.append(String.valueOf(day));
            averageCSVWriter.append(",");
            averageCSVWriter.append(String.valueOf(randomAllocations));
            averageCSVWriter.append(",");
            averageCSVWriter.append(String.valueOf(optimumAllocations));
        }

        // A pre-determined number of pairwise exchanges take place, during each exchange all agents have a chance to
        // trade with another agent.
        for (int exchange = 1; exchange <= exchanges; exchange++) {

            /*
             * With each exchange all agents form pairwise exchanges and are able to consider a trade with their
             * partner for one time slot.
             *
             * @param daysOfInterest Integer array containing the days be shown in graphs produced after the simulation.
             * @param additionalData Boolean value that configures the simulation to output the state of each agent
             *                       after each exchange and at the end of each day.
             * @param day Integer value representing the current day being simulated.
             * @param exchange Integer value representing the current exchange being simulated.
             * @param uniqueAgentTypes Integer ArrayList containing each unique agent type that exists when the
             *                         simulation begins.
             * @param agents Array List of all the agents that exist in the current simulation.
             * @param endOfRoundAverageSatisfactions Stores the average satisfaction for each agent type at the end of
             *                                       each round.
             * @param individualCSVWriter Writes additional data on the individual agents satisfaction after each
             *                            exchange when additional data is requested.
             * @exception IOException On input error.
             * @see IOException
             */
            new Exchange(
                    daysOfInterest,
                    additionalData,
                    day,
                    exchange,
                    uniqueAgentTypes,
                    agents,
                    endOfRoundAverageSatisfactions,
                    individualCSVWriter
            );
        }
        // The average end of day satisfaction is stored for each agent type to later be averaged and analysed.
        ArrayList<Double> endOfDayAverageSatisfaction = new ArrayList<>();
        endOfDayAverageSatisfaction.add((double) day);
        endOfDayAverageSatisfaction.add(randomAllocations);
        endOfDayAverageSatisfaction.add(optimumAllocations);

        // Store the end of day average satisfaction for each agent type.
        for (int uniqueAgentType : uniqueAgentTypes) {
            double typeAverageSatisfaction = CalculateSatisfaction.averageAgentSatisfaction(agents, uniqueAgentType);
            endOfDayAverageSatisfaction.add(typeAverageSatisfaction);
            if (additionalData) {
                averageCSVWriter.append(",");
                averageCSVWriter.append(String.valueOf(typeAverageSatisfaction));
            }
        }
        // Temporarily store the end of day average variance for each agent type.
        for (int uniqueAgentType : uniqueAgentTypes) {
            double typeAverageSatisfactionSD =
                    CalculateSatisfaction.averageSatisfactionStandardDeviation(agents, uniqueAgentType);
            endOfDayAverageSatisfaction.add(typeAverageSatisfactionSD);
        }
        if (additionalData) {
            averageCSVWriter.append("\n");
        }
        endOfDayAverageSatisfactions.add(endOfDayAverageSatisfaction);

        for (Integer uniqueAgentType : uniqueAgentTypes) {
            int populationQuantity = 0;
            for (Agent a : agents) {
                if (a.getAgentType() == uniqueAgentType) {
                    populationQuantity++;
                }
            }
            endOfDayPopulationDistributions.get(day - 1)
                    .get(uniqueAgentTypes.indexOf(uniqueAgentType)).add(populationQuantity);
        }

        // On days of interest, store the satisfaction for each agent at the end of the day to be added to violin plots.
        if (IntStream.of(daysOfInterest).anyMatch(val -> val == day)) {
            for (Agent a : agents) {
                ArrayList<Double> endOfDaySatisfaction = new ArrayList<>();
                endOfDaySatisfaction.add((double) day);
                endOfDaySatisfaction.add((double) a.getAgentType());
                endOfDaySatisfaction.add(a.calculateSatisfaction(null));
                endOfDaySatisfactions.add(endOfDaySatisfaction);
            }
        }

        /*
         * To facilitate social learning, for the number of the agents who are able to consider changing their strategy,
         * an Agent is selected at random, and then a second agent is selected to be observed. The first agent selected
         * checks whether their performance was weaker than the agent observed, if so they have a chance to copy the
         * strategy used by the observed agent in the previous day, with the likelihood of copying their strategy
         * proportional to the difference between their individual satisfactions.
         *
         * @param agents Array List of all the agents that exist in the current simulation.
         * @param slotsPerAgent Integer value representing the number of time slots each agent requires.
         * @param numberOfAgentsToEvolve Integer value representing the number of Agents who's strategy may change at
         *                               the end of each day.
         */
        new SocialLearning(agents, slotsPerAgent, numberOfAgentsToEvolve);
    }

    /**
     * Gives a random initial time slot allocation to an Agent based on the number of time slots it requests and the
     * time slots that are currently available.
     *
     * @param requestedTimeSlots The time slots that the Agent has requested.
     * @return ArrayList<Integer> Returns a list of time slots to allocated to the Agent.
     */
    private ArrayList<Integer> getRandomInitialAllocation(ArrayList<Integer> requestedTimeSlots) {
        ArrayList<Integer> timeSlots = new ArrayList<>();

        for (int requestedTimeSlot = 1; requestedTimeSlot <= requestedTimeSlots.size(); requestedTimeSlot++) {
            // Only allocate time slots if there are slots available to allocate.
            if (!availableTimeSlots.isEmpty()) {
                int selector = ResourceExchangeArena.random.nextInt(availableTimeSlots.size());
                int timeSlot = availableTimeSlots.get(selector);

                timeSlots.add(timeSlot);
                availableTimeSlots.remove(selector);
            }
        }
        return timeSlots;
    }
}
