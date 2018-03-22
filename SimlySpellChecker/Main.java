package SimlySpellChecker;

import java.awt.image.BufferedImage;
import java.sql.*;
import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.*;
import java.lang.StringBuilder;
import java.util.*;
import java.util.List;

import static java.lang.Character.isLetter;

public class Main extends JFrame implements ActionListener
{
    // define parameters to connect with database
    private static Connection c = null;
    private static Statement stmt = null;
    private static ResultSet res;

    // define my swing worker
    private SwingWorker<Void, Void> worker;

    // start the application
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run()
            {
                // try to connect to the database
                try
                {
                    Class.forName("org.postgresql.Driver");
                    c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/testdb", "postgres", "321");
                    System.out.println("Opened database successfully :)");

                    stmt = c.createStatement();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    System.err.println(e.getClass().getName() + ": " + e.getMessage() + " :(");
                }

                // create the window frame
                new Main();
            }
        });
    }

    // define the lists for typos and exceptions
    private List<Typo> listOfTypos = new ArrayList<>();
    private List<String> listOfExceptions = new ArrayList<>();

    // define of all the components for the current frame
    private CardLayout cl = new CardLayout(); // Thank you Branislav Lazic for the tutorials on Card and BoxLayout
    private JPanel pCardL = new JPanel();     // https://github.com/BranislavLazic
    private JPanel pBtns = new JPanel();
    private JPanel pLoading = new JPanel();
    private JPanel pReplace = new JPanel();
    private JPanel pRepReplace, pRepIgnore, pRepSkip, pRepErrors;
    private JPanel pDone = new JPanel();
    private JTextArea textArea = new JTextArea(23, 5);
    private JScrollPane scroll = new JScrollPane(textArea); // add the text area to the scroll pane
    private JRadioButton rBG = new JRadioButton("BG");
    private JRadioButton rENG = new JRadioButton("ENG");
    private ButtonGroup btnG = new ButtonGroup();
    private JButton checkBtn = new JButton("Check");
    private JButton replaceBtn = new JButton("Replace");
    private JButton ignoreBtn = new JButton( "Ignore");
    private JButton skipBtn = new JButton( "Skip");
    private JButton doneBtn = new JButton( "Done");
    private JLabel replaceLbl = new JLabel("Replace ____ with");
    private JLabel ignoreLbl = new JLabel( "Always ignore ____");
    private JLabel skipLbl = new JLabel( " Ignore ___ only once");
    private JLabel numErrLbl = new JLabel("___ errors");
    private JLabel doneLbl = new JLabel("<html>0 Errors left :)" +
                                        "<br>Once you press this button your text will be deleted and" +
                                        "<br>you can begin another spell check." +
                                        "<br>Make sure to copy your text now before leaving :)</html>");
    private JLabel loadingLbl = new JLabel();
    private JTextField replaceField = new JTextField(10);

    // define my text area highlighter
    private Highlighter highlight = textArea.getHighlighter();

    private Main()
    {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container container = this.getContentPane();

        // add listeners
        rBG.addActionListener(this);
        rENG.addActionListener(this);
        checkBtn.addActionListener(this);

        // add the radio buttons to the button group
        btnG.add(rBG);
        btnG.add(rENG);
        rENG.setSelected(true);

        // make the text automatically go to a new line at the crossing word
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        // set the scroll size and add to top of the frame, num of columns doesn't matter so it's 0
        //scroll.setPreferredSize(new Dimension(0, 380));
        container.add(scroll, BorderLayout.PAGE_START);

        // place the components for the checking panel i.e pBtns. Adequate name I know.
        pBtns.setLayout(new BorderLayout());
        /*pBtns.add(rBG);
        pBtns.add(rENG);*/
        pBtns.add(checkBtn, BorderLayout.PAGE_START);


        // Thank you: https://alvinalexander.com/blog/post/jfc-swing/use-animated-gif-image-in-jfc-swing-application
        // Gif used under free license from https://icons8.com/preloaders/en/horizontal/glass-lines/

        // create the components for the loading panel.
        ImageIcon loadingGif = new ImageIcon(this.getClass().getResource("loading.gif"));
        loadingLbl.setIcon(loadingGif);
        pLoading.add(loadingLbl);

        // create the components for the replace panel
        pRepReplace = new JPanel();
        pRepReplace.setLayout(new FlowLayout(FlowLayout.LEFT));
        pRepReplace.add(replaceLbl);
        pRepReplace.add(replaceField);

        pRepIgnore = new JPanel();
        pRepIgnore.setLayout(new FlowLayout(FlowLayout.LEFT));
        pRepIgnore.add(ignoreLbl);

        pRepSkip = new JPanel();
        pRepSkip.setLayout(new FlowLayout(FlowLayout.LEFT));
        pRepSkip.add(skipLbl);

        pRepErrors = new JPanel();
        pRepErrors.setLayout(new FlowLayout(FlowLayout.LEFT));
        pRepErrors.add(numErrLbl);

        // grid align these replace panels in 4 rows of 2 columns 0 hgap 4 vgap
        pReplace.setLayout(new GridLayout(4,2, 0, 4));

        pReplace.add(pRepReplace);
        pReplace.add(replaceBtn);
        pReplace.add(pRepIgnore);
        pReplace.add(ignoreBtn);
        pReplace.add(pRepSkip);
        pReplace.add(skipBtn);
        pReplace.add(pRepErrors);

        // Listen to the buttons in pReplace
        replaceBtn.addActionListener(this);
        ignoreBtn.addActionListener(this);
        skipBtn.addActionListener(this);

        // create the components for the done panel
        pDone.setLayout(new FlowLayout(FlowLayout.LEFT));
        pDone.add(doneLbl);
        pDone.add(doneBtn);

        doneBtn.addActionListener(this);

        // place the panels for checking and replacing in the cardlayout containing panel
        pCardL.setLayout(cl);
        pCardL.add(pBtns, "pBtns");
        pCardL.add(pLoading, "pLoading");
        pCardL.add(pReplace, "pReplace");
        pCardL.add(pDone, "pDone");
        container.add(pCardL, BorderLayout.CENTER);

        // on startup show pBtns
        cl.show(pCardL, "pBtns");

        // set the size of the frame
        this.setSize(500,510);
        this.setResizable(false);

        // make the frame centered to the screen
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width/2 - this.getSize().width/2,
                              dim.height/2 - this.getSize().height/2);

        // set the visibility of the frame
        this.setVisible(true);

        /*
            * TODO
            * test if StringBuilder append is faster than + operand
            * Make it pretty
            * Make it better
         */
    }

    public void actionPerformed(ActionEvent ae)
    {
        Object source = ae.getSource();

        if (source == checkBtn)
        {
            if (rBG.isSelected())
            {
                JOptionPane.showMessageDialog(this, "BG spell check not yet supported");
            }
            else
            {
                // show the loading icon
                cl.show(pCardL, "pLoading");

                /*
                 * checkSpelling() might take an enormous ammount of time, if the text is long
                 * to combat this I run it in the background using swing worker
                 * BIG thanks to Albert Attard and his tutorial on the topic:
                 * https://www.youtube.com/watch?v=qbrE6idMsWU
                */
                worker = new SwingWorker<Void, Void>() {

                    // check the spelling
                    @Override
                    protected Void doInBackground() {
                        checkSpelling();
                        return null;
                    }

                    // When spell check is complete
                    @Override
                    protected void done() {

                        // if there are no typos show Done panel
                        if (listOfTypos.size() == 0)
                        {
                            highlight.removeAllHighlights();
                            cl.show(pCardL, "pDone");
                        }
                        else
                        {
                            // else show the interface for replacing typos
                            cl.show(pCardL,"pReplace");

                            // refresh the labels and highlight the first typo in listOfTypos
                            refreshHighlightAndFocus();
                        }
                    }
                };
                worker.execute();
            }
        }

        else if (source == replaceBtn)
        {
            // fix the spelling of the first typo in listOfTypos
            replaceTypo();
        }

        else if (source == ignoreBtn)
        {
            // make the current typo an exception, don't check for it again and remove it from the listOfTypos
            handleTypoException(listOfTypos.get(0).getTypo());
        }

        else if (source == skipBtn)
        {
            // remove this typo from the listOT refresh for the next one
            listOfTypos.remove(0);

            if (listOfTypos.size() > 0)
            {
                refreshHighlightAndFocus();
            }
            else
            {
                highlight.removeAllHighlights();
                cl.show(pCardL, "pDone");
            }
        }

        else if (source == doneBtn)
        {
            textArea.setText("");
            textArea.setEditable(true);
            cl.show(pCardL, "pBtns");
        }
    }

    private void checkSpelling()
    {
        // set the text area to not be editable during the check process
        textArea.setEditable(false);

        // get the text from the area
        String text = textArea.getText();
        String word = "";

        // newWord is exactly like word only with double ' used for checking the dictionary in the database
        String newWord = "";

        int i;

        // if there is any text check the text word for word
        if (text.length() > 0)
        {
            for (i = 0; i < text.length(); i++)
            {
                // if the current symbol is a letter put it in word and newWord
                if (isLetter(text.charAt(i)))
                {
                    word = word + (text.charAt(i));
                    newWord = newWord + (text.charAt(i));
                }

                else if (text.charAt(i)=='\'')
                {
                    // need to double the apostrophe for the database comparison
                    word = word + "\'";
                    newWord = newWord + "\'\'";
                }

                else
                {
                    // if the word is NOT empty check it for typos
                    if (!word.equals(""))
                    {
                        // convert word to lower case
                        word = word.toLowerCase();

                        // if word is NOT in the list of exceptions AND dictionary, then it's invalid
                        if (!listOfExceptions.contains(word) && !spellCheckThisWord(newWord))
                        {
                            Typo typo = new Typo(word, (i - word.length()), i);
                            listOfTypos.add(typo);
                        }

                        // empty the word string to search for  a new word
                        word = "";
                        newWord = "";
                    }
                }
            }

            // if the text area ended with a letter then check the last word in the text area
            if (!word.equals("") && !listOfExceptions.contains(word.toLowerCase()) && !spellCheckThisWord(newWord))
            {
                Typo typo = new Typo(word.toLowerCase(), (i - word.length()), i);
                listOfTypos.add(typo);
            }
        }
    }

    // checks the replacement word before replacing the typo from the text area
    private void replaceTypo()
    {
        // get the replacement word from the field
        String word = replaceField.getText();

        // newWord is used for the database comparison
        String newWord = "";

        // check word for any illegal symbols
        boolean check = true;

        // if the word contains a symbol which is not a letter or apostrophe check = false
        for (int i = 0; i < word.length(); i++)
        {
            if (isLetter(word.charAt(i)))
            {
                newWord = newWord + word.charAt(i);
            }
            else if (word.charAt(i) == '\'')
            {
                // need to double the apostrophe for the database comparison
                newWord = newWord + "\'\'";
            }
            else
            {
                newWord = newWord + word.charAt(i);
                check = false;
            }
        }

        // if the word is invalid show error dialog message
        if (word.equals("") || !check || (!listOfExceptions.contains(word.toLowerCase()) && !spellCheckThisWord(newWord)))
        {
            int choice = JOptionPane.showConfirmDialog(this,
                                                    "\" " + word + " \" not found in the dictionary." +
                                                       " Do you still want to use this word?" +
                                                       " If so I will remember it.",
                                                       "Error", JOptionPane.YES_NO_OPTION);

            // if the user wants to replace typo with an "invalid" word, replace it and make that word an exception
            if (choice == JOptionPane.YES_OPTION)
            {
                // replace the typo with word
                replaceTypoNoCheck(listOfTypos.get(0), word);

                // make word an exception and remove all instances of it from listOfTypos
                handleTypoException(word);
            }
            // else clear the field and wait for a new replace word
            else
            {
                replaceField.setText("");
            }
        }
        else
        {
            replaceTypoNoCheck(listOfTypos.get(0), word);
        }
    }

    // replace typo with word no questions asked
    private void replaceTypoNoCheck(Typo typo, String word)
    {
        // calculate the typo's length
        int typoLength = typo.getIndexEnd() - typo.getIndexBegin();

        // replace the typo with word in text area
        textArea.replaceRange(word, typo.getIndexBegin(), typo.getIndexEnd());

        // if the length of the word is > than the typo, fix the indexes of all the other typos in the list
        if (word.length() > typoLength)
        {
            int difference = word.length() - typoLength;
            Typo tempTypo;

            for (int i = 1; i < listOfTypos.size(); i++)
            {
                tempTypo = listOfTypos.get(i);
                tempTypo.setIndexB(tempTypo.getIndexBegin() + difference);
                tempTypo.setIndexE(tempTypo.getIndexEnd() + difference);
            }
        }
        else if (word.length() < typoLength)
        {
            int difference = typoLength - word.length();
            Typo tempTypo;

            for (int i = 1; i < listOfTypos.size(); i++)
            {
                tempTypo = listOfTypos.get(i);
                tempTypo.setIndexB(tempTypo.getIndexBegin() - difference);
                tempTypo.setIndexE(tempTypo.getIndexEnd() - difference);
            }
        }

        // remove the typo from the list
        listOfTypos.remove(0);

        // if any typos are left refresh the panel
        if (listOfTypos.size() > 0)
        {
            refreshHighlightAndFocus();
        }
        // else go to finished panel
        else
        {
            highlight.removeAllHighlights();
            cl.show(pCardL, "pDone");
        }
    }

    private boolean spellCheckThisWord(String word)
    {
        // convert word to lower case
        word = word.toLowerCase();

        if (word.charAt(0) == '\'')
        {
            return false;
        }
        else
        {
            // compare word to every word in the database starting with the same letter
            try
            {
                String sql = "SELECT * FROM testarrays WHERE fchar = '" + word.charAt(0) + "' AND words @> '{" + word + "}';";
                res = stmt.executeQuery(sql);

                // if the word is NOT found in the dictionary return false
                if (!res.next())
                {
                    return false;
                }
            }
            catch (Exception e)
            {
                System.err.println(e.getClass().getName() + ": " + e.getMessage() + " :(");
            }
        }

        // if passed all the checks return true
        return true;
    }

    public void handleTypoException(String typoEx)
    {
        typoEx = typoEx.toLowerCase();

        // save the current typo in the list of exceptions
        listOfExceptions.add(typoEx);

        // create a temporary list of typos
        List<Typo> tempListOfTypos = new ArrayList<>();

        // tempLOT should contain every object from listOT that is NOT typoEx
        for (int i = 0; i < listOfTypos.size(); i++)
        {
            if (!(listOfTypos.get(i).getTypo().equals(typoEx)))
            {
                tempListOfTypos.add(listOfTypos.get(i));
            }
        }

        // clear listOT
        listOfTypos.clear();

        // if any typos were left, add them to the listOfTypos, clear the tempLOT and refresh
        if (tempListOfTypos.size() > 0)
        {
            listOfTypos.addAll(tempListOfTypos);
            tempListOfTypos.clear();
            refreshHighlightAndFocus();
        }
        // if no more typos are left go to Done panel
        else
        {
            highlight.removeAllHighlights();
            cl.show(pCardL, "pDone");
        }
    }

    private void refreshHighlightAndFocus()
    {
        // get the first typo
        Typo typo = listOfTypos.get(0);

        String shortestWord = typo.getTypo();
        String shortWord = typo.getTypo();

        // Thank you to /u/MadProgrammer for the example how to find string length in pixels
        // https://stackoverflow.com/questions/18327825/how-do-i-calculate-the-width-of-a-string-in-pixels
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        FontMetrics fm = g2d.getFontMetrics();

        // The row for Replace can only show a word up to 18 pixels - 3 for the period = 15 px
        // The rows for ignore and skip can take a word up to 120 pixels - 3 for the period = 127 px
        // if the typo is longer than 15px but shorter than 117, shorten it only for the replace row
        if (fm.stringWidth(typo.getTypo()) > 18 && fm.stringWidth(typo.getTypo()) <= 120)
        {
            for (int i = 1; fm.stringWidth(typo.getTypo().substring(0, i)) < 15; i++)
            {
                shortestWord = typo.getTypo().substring(0, i) + ".";
            }
        }
        // if the typo is longer than 117px, shorten it for the replace row AND the other 2 rows
        else if (fm.stringWidth(typo.getTypo()) > 120)
        {
            for (int i = 1; fm.stringWidth(typo.getTypo().substring(0, i)) < 117; i++)
            {
                if (fm.stringWidth(typo.getTypo().substring(0, i)) < 15)
                {
                    shortestWord = typo.getTypo().substring(0, i) + ".";
                }
                shortWord = typo.getTypo().substring(0, i) + ".";
            }
        }
        g2d.dispose();

        // set the labels to the current typo information
        replaceLbl.setText("Replace \" " + shortestWord + " \" with: ");
        ignoreLbl.setText("Always ignore \" " + shortWord + " \"");
        skipLbl.setText("Ignore \" " + shortWord + " \" only once");
        numErrLbl.setText(listOfTypos.size() + " Errors");

        // highlight typo
        // Thank you user Manji https://stackoverflow.com/questions/5949524/highlight-sentence-in-textarea

        highlight.removeAllHighlights();
        try
        {
            highlight.addHighlight(typo.getIndexBegin(), typo.getIndexEnd(), DefaultHighlighter.DefaultPainter);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        // delete the replacement word from the text field
        replaceField.setText("");

        // focus cursor on the textfield after pressing the button for better UX when replacing multiple typos
        replaceField.requestFocus();

        // focus view on the first typo
        focusOnTypo(typo);
    }

    private void focusOnTypo(Typo typo)
    {
        // Thank you to user MadProgrammer
        // https://stackoverflow.com/questions/13437865/java-scroll-to-specific-text-inside-jtextarea
        try
        {
            Rectangle viewRect = textArea.modelToView(typo.getIndexBegin());
            textArea.scrollRectToVisible(viewRect);
            textArea.setCaretPosition(typo.getIndexBegin());
            textArea.moveCaretPosition(typo.getIndexBegin());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

