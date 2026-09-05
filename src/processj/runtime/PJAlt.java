package processj.runtime;

import java.util.ArrayList;
import java.util.List;

// new added
import java.util.concurrent.ThreadLocalRandom;
import java.util.Collections;

public class PJAlt {
    
    /** Can be skips, timers or channel-reads */
    private List<Object> guards;
    
    /** Boolean guards */
    private List<Boolean> bguards;
    
    /** Process declaring the 'alt' */
    private PJProcess process;
    
    private List<AltGuard> dynamicAlts;
    
    public static final String SKIP = "skip";
    
    public enum SelectionPolicy {
        RANDOM,
        SOURCE_ORDER,
        NUMERIC
    }

    /** Snapshot containing one priority for every guard. */
    private List<Integer> priorities = Collections.emptyList();

    /** Selection behavior for this ALT. */
    private SelectionPolicy selectionPolicy = SelectionPolicy.RANDOM;

    public PJAlt(PJProcess p) {
        process = p;
        guards = new ArrayList<>();
        bguards = new ArrayList<>();
    }
    
    public PJAlt(int count, PJProcess p) {
        process = p;
        guards = new ArrayList<>(count);
        bguards = new ArrayList<>(count);
    }
    
    // public boolean setGuards(List<Boolean> bguards, List<Object> guards) {
    //     this.guards = guards;
    //     this.bguards = bguards;
        
    //     for (Boolean b : bguards)
    //         if (b.booleanValue())
    //             return true;
        
    //     return false;
    // }

    /*
    * Keep the old API for replicated ALTs and previously generated code.
    */
    public boolean setGuards(
            List<Boolean> bguards,
            List<Object> guards) {

        if (guards == null) {
            throw new IllegalArgumentException(
                "ALT guards cannot be null");
        }

        return setGuards(
            bguards,
            guards,
            Collections.nCopies(
                guards.size(),
                Integer.valueOf(0)),
            SelectionPolicy.RANDOM);
    }

    /*
    * Store the guards, priorities, and selection policy.
    */
    public boolean setGuards(
            List<Boolean> bguards,
            List<Object> guards,
            List<Integer> priorities,
            SelectionPolicy selectionPolicy) {

        if (bguards == null ||
            guards == null ||
            priorities == null ||
            selectionPolicy == null) {

            throw new IllegalArgumentException(
                "ALT guards, priorities, and selection policy cannot be null");
        }

        if (bguards.size() != guards.size() ||
            priorities.size() != guards.size()) {

            throw new IllegalArgumentException(
                "ALT boolean guards, guards, and priorities must have the same size");
        }

        for (Integer priority : priorities) {
            if (priority == null) {
                throw new IllegalArgumentException(
                    "ALT priority values cannot be null");
            }
        }

        this.guards = guards;
        this.bguards = bguards;

        /*
        * Copy the priorities so they remain unchanged while the ALT
        * enables, yields, and disables its guards.
        */
        this.priorities = new ArrayList<>(priorities);
        this.selectionPolicy = selectionPolicy;

        for (Boolean b : bguards) {
            if (b.booleanValue()) {
                return true;
            }
        }

        return false;
    }
    
    public void setDynamicAlts(List<AltGuard> dynamicAlts) {
        this.dynamicAlts = dynamicAlts;
    }
    
    public AltGuard getDynamicAlts(int index) {
        return dynamicAlts.get(index);
    }
    
    // // *************************************************************************
    // // new added function
    // private int chooseFromReadySet(List<Integer> ready) {
    //     if (ready.isEmpty()) {
    //         return -1;
    //     }

    //     int position = ThreadLocalRandom.current().nextInt(ready.size());
    //     return ready.get(position);
    // }
    // // *************************************************************************

    
    private int chooseFromReadySet(List<Integer> ready) {
        if (ready.isEmpty()) {
            return -1;
        }

        switch (selectionPolicy) {
            case SOURCE_ORDER: {
                int selected = ready.get(0);

                /*
                * A smaller index means the guard appeared earlier
                * in the ProcessJ source.
                */
                for (int candidate : ready) {
                    if (candidate < selected) {
                        selected = candidate;
                    }
                }

                return selected;
            }

            case NUMERIC: {
                int selected = ready.get(0);
                int selectedPriority = priorities.get(selected);

                for (int candidate : ready) {
                    int candidatePriority = priorities.get(candidate);

                    /*
                    * Larger numbers have higher priority.
                    * Equal values are resolved by source order.
                    */
                    if (candidatePriority > selectedPriority ||
                        (candidatePriority == selectedPriority &&
                        candidate < selected)) {

                        selected = candidate;
                        selectedPriority = candidatePriority;
                    }
                }

                return selected;
            }

            case RANDOM:
            default: {
                int position =
                    ThreadLocalRandom.current().nextInt(ready.size());

                return ready.get(position);
            }
        }
    }
    
    
    
    @SuppressWarnings("rawtypes")

    // Matt enable()
    // public int enable() {
    //     for (int i = 0; i < guards.size(); ++i) {
    //         // If no boolean guard is ready then continue
    //         if (!bguards.get(i))
    //             continue;
    //         // A skip?
    //         if (guards.get(i) == PJAlt.SKIP) {
    //             process.setReady();
    //             return i;
    //         }
    //         // A channel?
    //         if (guards.get(i) instanceof PJChannel) {
    //             PJChannel chan = (PJChannel) guards.get(i);
    //             if (chan.altGetWriter(process) != null) {
    //                 process.setReady();
    //                 return i;
    //             }
    //         }
    //         // A timer?
    //         if (guards.get(i) instanceof PJTimer) {
    //             // TODO: Shouldn't this be formally verified??
    //             PJTimer t = (PJTimer) guards.get(i);
    //             if (t.getDelay() <= 0L) {
    //                 process.setReady();
    //                 t.expire();
    //                 return i;
    //             } else {
    //                 try {
    //                     t.start();
    //                 } catch (InterruptedException e) {
    //                     e.printStackTrace();
    //                 }
    //             }
    //         }
    //     }
    //     return -1;
    // }
    
    public int enable() {

        List<Integer> ready = new ArrayList<>();

        for (int i = 0; i < guards.size(); ++i) {
            if (!bguards.get(i)) continue;

            Object g = guards.get(i);

            if (g == PJAlt.SKIP) {
                ready.add(i);
                continue;
            }

            if (g instanceof PJChannel) {
                PJChannel chan = (PJChannel) g;
                if (chan.altGetWriter(process) != null) {
                    ready.add(i);
                }
                continue;
            }

            if (g instanceof PJTimer) {
                PJTimer t = (PJTimer) g;
                if (t.getDelay() <= 0L) {
                    t.expire();
                    ready.add(i);
                } else {
                    try {
                        t.start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (ready.isEmpty()) return -1;

        process.setReady();
        return chooseFromReadySet(ready); 
    }

    @SuppressWarnings("rawtypes")

    // matt disable()
    // public int disable(int i) {
    //     int selected = -1;
    //     if (i == -1)
    //         i = guards.size() - 1;
    //     for (int j = i; j >= 0; --j) {
    //         // If no boolean guard is ready then continue
    //         if (!bguards.get(j))
    //             continue;
    //         // A skip?
    //         if (guards.get(j) == PJAlt.SKIP)
    //             selected = j;
    //         // A channel?
    //         if (guards.get(j) instanceof PJChannel) { 
    //             // No race condition on this channel as it is a one-to-one and only THIS
    //             // process has access to it. This simply means that we are de-registering
    //             // from the channel for now, but may still read from it if selected is not
    //             // updated with a value < j.
    //             PJChannel chan = (PJChannel) guards.get(j);
    //             if (chan.setReaderGetWriter(null) != null)
    //                 selected = j;
    //         }
    //         // A timer?
    //         if (guards.get(j) instanceof PJTimer) {
    //             // TODO: Shouldn't this be formally verified??
    //             PJTimer timer = (PJTimer) guards.get(j);
    //             if (timer.isExpired())
    //                 selected = j;
    //             else
    //                 timer.kill();
    //         }
    //     }
    //     return selected;
    // }

    public int disable(int chosen) {
        List<Integer> stillReady = new ArrayList<>();

        for (int j = guards.size() - 1; j >= 0; --j) {
            if (!bguards.get(j)) continue;

            Object g = guards.get(j);

            if (g == PJAlt.SKIP) {
                stillReady.add(j);
                continue;
            }

            if (g instanceof PJChannel) {
                PJChannel chan = (PJChannel) g;
                if (chan.setReaderGetWriter(null) != null) {
                    stillReady.add(j);
                }
                continue;
            }

            if (g instanceof PJTimer) {
                PJTimer t = (PJTimer) g;
                if (t.isExpired()) {
                    stillReady.add(j);
                } else {
                    t.kill();
                }
            }
        }

        // if (chosen != -1 && stillReady.contains(chosen)) {
        //     return chosen;
        // }

        // if (stillReady.isEmpty()) return -1;
        // return chooseFromReadySet(stillReady);
    /*
    * Preserve an initial random choice if it remains ready.
    *
    * Prioritized modes must reconsider all currently ready guards:
    * a higher-priority guard may have become ready during yield().
    */
    if (selectionPolicy == SelectionPolicy.RANDOM &&
        chosen != -1 &&
        stillReady.contains(chosen)) {

        return chosen;
    }

    return chooseFromReadySet(stillReady);
    
    
    }

}
