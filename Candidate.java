public class Candidate {
    private int candidateNumber;
    private String name;
    private String party;

    public Candidate(int candidateNumber, String name, String party){
        this.candidateNumber = candidateNumber;
        this.name = name;
        this.party = party;
    }

    public int getNumber(){
        return candidateNumber;
    }

    public String getName(){
        return name;
    }

    public String getParty(){
        return party;
    }
}
