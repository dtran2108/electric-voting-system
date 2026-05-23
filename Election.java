import java.util.ArrayList;
import java.util.List;

public class Election {
    private String electionId;
    private boolean isOpen;
    private List<Candidate> candidates;
    private int maxCandidates;

    public Election(String electionId, int maxCandidates){
        this.electionId = electionId;
        this.maxCandidates = 5;
        this.isOpen = false;
        this.candidates = new ArrayList<>();
    }

    public void open(){
        this.isOpen = true;
    }

    public void close(){
        this.isOpen = false;
    }

    public List<Candidate> getCandidates(){
        return candidates;
    }
}