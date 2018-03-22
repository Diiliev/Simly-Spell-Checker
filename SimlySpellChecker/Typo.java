package SimlySpellChecker;

public class Typo
{
    // the word
    private String typo;

    // the indexes of the beginning of the word and it's end. Need it for highlighter
    private int indexB, indexE;

    public Typo(String typo, int indexB, int indexE)
    {
        this.typo = typo;
        this.indexB = indexB;
        this.indexE = indexE;
    }

    // get methods for my private fields
    public String getTypo()
    {
        return this.typo;
    }

    public int getIndexBegin()
    {
        return this.indexB;
    }

    public int getIndexEnd()
    {
        return this.indexE;
    }

    // set methods for my private fields
    public void setIndexB(int newIndexB)
    {
        this.indexB = newIndexB;
    }

    public void setIndexE(int newIndexE)
    {
        this.indexE = newIndexE;
    }
}
