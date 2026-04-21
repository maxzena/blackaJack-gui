import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

public class BlackjackGUI extends JFrame {

    // ══════════════════════════════════════════════════════════════════════════
    //  COSTANTI
    // ══════════════════════════════════════════════════════════════════════════
    private static final String CARD_PATH  = "C:\\Users\\PC\\Desktop\\cards\\PNG-cards-1.3\\";
    private static final String USERS_FILE = "utenti.txt";
    private static final String[] SEMI   = {"clubs","diamonds","hearts","spades"};
    private static final String[] VALORI = {"1","2","3","4","5","6","7","8","9","10","j","q","k"};

    private static final Color FELT_DARK  = new Color(0, 85, 38);
    private static final Color FELT_MID   = new Color(0, 112, 52);
    private static final Color GOLD       = new Color(212, 175, 55);
    private static final Color GOLD_LIGHT = new Color(255, 215, 80);
    private static final Color PANEL_BG   = new Color(28, 28, 28);
    private static final Color INPUT_BG   = new Color(42, 42, 42);
    private static final Color ERR_COLOR  = new Color(220, 60, 60);
    private static final Color OK_COLOR   = new Color(60, 200, 60);

    private static final Font LOGO_FONT  = new Font("Georgia", Font.BOLD, 38);
    private static final Font LABEL_FONT = new Font("Georgia", Font.PLAIN, 13);
    private static final Font INPUT_FONT = new Font("Courier New", Font.PLAIN, 14);
    private static final Font BTN_FONT   = new Font("Arial Black", Font.BOLD, 15);
    private static final Font TITLE_FONT = new Font("Georgia", Font.BOLD, 15);
    private static final Font SCORE_FONT = new Font("Courier New", Font.BOLD, 22);
    private static final Font INFO_FONT  = new Font("Georgia", Font.PLAIN, 14);

    private static final int[]   CHIP_VALUES = {25, 50, 100, 500, 1000};
    private static final Color[] CHIP_COLORS = {
            new Color(200,50,50), new Color(50,100,200),
            new Color(40,170,40), new Color(40,40,40), new Color(200,170,50)
    };

    // Dimensioni carta
    private static final int CW = 85, CH = 122;

    // ══════════════════════════════════════════════════════════════════════════
    //  STATO SESSIONE
    // ══════════════════════════════════════════════════════════════════════════
    private String utenteLoggato = null;
    private int    saldo = 1000;

    private final Random rnd = new Random();
    private int puntata = 0;
    private final ArrayList<int[]> manoGiocatore = new ArrayList<>();
    private final ArrayList<int[]> manoDealer    = new ArrayList<>();
    private boolean cartaNascostaVisibile = false;
    private boolean inMano = false;

    // ── CardLayout principale ─────────────────────────────────────────────────
    private JPanel     cardLayout;
    private CardLayout cards;

    // Schermata login
    private JTextField    loginUserField;
    private JPasswordField loginPassField;
    private JLabel        loginMsgLabel;

    // Schermata signup
    private JTextField    signupUserField;
    private JPasswordField signupPassField;
    private JPasswordField signupPass2Field;
    private JLabel        signupMsgLabel;

    // ── Schermata gioco ───────────────────────────────────────────────────────
    private JLayeredPane   gameLayered;   // layered pane per animazioni
    private JPanel         tablePanel;   // tavolo (sfondo feltro)
    private JPanel         dealerCardPanel;
    private JPanel         playerCardPanel;
    private JLabel         dealerScoreLabel;
    private JLabel         playerScoreLabel;
    private JLabel         playerZoneLabel;
    private JLabel         saldoLabel;
    private JLabel         messageLabel;
    private JLabel         welcomeLabel;
    private JButton        btnHit, btnStand, btnDeal, btnReset;
    private JTextField     puntataField;
    private JPanel         chipPanel;

    // Posizione del mazzo (centro destra del tavolo) – calcolata al runtime
    private Point deckScreenPos = new Point(900, 250);

    // ══════════════════════════════════════════════════════════════════════════
    public BlackjackGUI() {
        super("Blackjack");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(1920, 1080);
        setMinimumSize(new Dimension(950, 720));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        cards = new CardLayout();
        cardLayout = new JPanel(cards);
        cardLayout.add(buildLoginScreen(),  "LOGIN");
        cardLayout.add(buildSignupScreen(), "SIGNUP");
        cardLayout.add(buildGameScreen(),   "GAME");
        add(cardLayout, BorderLayout.CENTER);

        cards.show(cardLayout, "LOGIN");
        setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SCHERMATA LOGIN
    // ══════════════════════════════════════════════════════════════════════════
    // Carte mostrate nell'animazione shuffle della login screen
    private final ImageIcon[] shuffleIcons  = new ImageIcon[9];
    private final double[]    shuffleAngles = new double[9];
    private final double[]    shuffleX      = new double[9];
    private final double[]    shuffleY      = new double[9];
    private Timer             shuffleTimer;
    private int               shuffleFrame  = 0;
    private JPanel            loginShufflePanel;

    private JPanel buildLoginScreen() {
        String[] specimen = {
                "1_of_spades","k_of_hearts","q_of_diamonds","j_of_clubs",
                "10_of_spades","9_of_hearts","8_of_diamonds","7_of_clubs","6_of_spades"
        };
        for (int i=0;i<9;i++) {
            File f = new File(CARD_PATH + specimen[i] + ".png");
            if (f.exists()) { try { shuffleIcons[i]=new ImageIcon(ImageIO.read(f)); } catch(Exception ig){} }
            if (shuffleIcons[i]==null) shuffleIcons[i]=placeholder("?","spades",false);
        }

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g;
                g2.setPaint(new GradientPaint(0,0,new Color(8,8,8),getWidth(),getHeight(),FELT_DARK));
                g2.fillRect(0,0,getWidth(),getHeight());
            }
        };

        // Logo
        JLabel logo = new JLabel("♠  BLACKJACK  ♠", SwingConstants.CENTER);
        logo.setFont(new Font("Georgia",Font.BOLD,56));
        logo.setForeground(GOLD_LIGHT);
        logo.setBorder(BorderFactory.createEmptyBorder(40,0,6,0));

        // Pannello carte largo tutta la schermata
        final double[] baseAngles = {-24,-16,-8,0,8,16,24,-12,12};
        loginShufflePanel = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int cw=140, ch=200;
                for (int i=0;i<9;i++) {
                    if (shuffleIcons[i]==null) continue;
                    Graphics2D g3=(Graphics2D)g2.create();
                    int cx=(int)shuffleX[i]+cw/2, cy=(int)shuffleY[i]+ch/2;
                    g3.rotate(Math.toRadians(shuffleAngles[i]),cx,cy);
                    g3.setColor(new Color(0,0,0,110));
                    g3.fillRoundRect((int)shuffleX[i]+7,(int)shuffleY[i]+7,cw,ch,14,14);
                    g3.drawImage(shuffleIcons[i].getImage(),(int)shuffleX[i],(int)shuffleY[i],cw,ch,null);
                    g3.setColor(new Color(212,175,55,160));
                    g3.setStroke(new BasicStroke(2f));
                    g3.drawRoundRect((int)shuffleX[i],(int)shuffleY[i],cw-1,ch-1,14,14);
                    g3.dispose();
                }
            }
            @Override public Dimension getPreferredSize(){ return new Dimension(0,240); }
        };
        loginShufflePanel.setOpaque(false);

        // Posizioni iniziali
        for (int i=0;i<9;i++) {
            shuffleAngles[i]=baseAngles[i];
            shuffleX[i]=80+i*160;
            shuffleY[i]=20;
        }

        if (shuffleTimer!=null) shuffleTimer.stop();
        shuffleTimer = new Timer(40, e -> {
            shuffleFrame++;
            int W=loginShufflePanel.getWidth(); if(W<10) W=1600;
            double spacing=(W-160.0)/8.0;
            for (int i=0;i<9;i++) {
                shuffleAngles[i]=baseAngles[i]+9*Math.sin(shuffleFrame*0.06+i*1.1);
                shuffleX[i]=40+i*spacing+4*Math.sin(shuffleFrame*0.05+i*0.8);
                shuffleY[i]=16+8*Math.sin(shuffleFrame*0.07+i*1.3);
            }
            loginShufflePanel.repaint();
        });
        shuffleTimer.start();

        JPanel northArea=new JPanel(new BorderLayout()); northArea.setOpaque(false);
        northArea.add(logo,              BorderLayout.NORTH);
        northArea.add(loginShufflePanel, BorderLayout.CENTER);
        root.add(northArea, BorderLayout.NORTH);

        // Form login
        JPanel form=buildAuthForm();
        JLabel title=centeredLabel("LOG IN",new Font("Georgia",Font.BOLD,24),GOLD_LIGHT);
        form.add(title,0); form.add(Box.createVerticalStrut(20),1);
        loginUserField=styledTextField(); loginPassField=styledPassField(); loginMsgLabel=msgLabel();
        form.add(fieldRow("Utente:",   loginUserField)); form.add(Box.createVerticalStrut(12));
        form.add(fieldRow("Password:", loginPassField)); form.add(Box.createVerticalStrut(18));
        form.add(loginMsgLabel); form.add(Box.createVerticalStrut(10));
        JButton btn=actionBtn("ENTRA",GOLD,new Color(25,25,25));
        btn.addActionListener(e->doLogin()); loginPassField.addActionListener(e->doLogin());
        form.add(wrap(btn)); form.add(Box.createVerticalStrut(14));
        JLabel link=hyperLabel("Non hai un account? SIGN UP");
        link.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){clearSignup();cards.show(cardLayout,"SIGNUP");}
        });
        form.add(link);

        // Chip verticali: ogni pila cresce verso l'alto, chip più grandi
        JPanel chipArea=new JPanel(null) {
            final Color[]  cc={new Color(200,50,50),new Color(50,100,200),new Color(40,170,40),new Color(40,40,40),new Color(200,170,50)};
            final String[] ll={"25","50","100","500","1K"};
            final int[]    nn={10,8,12,5,9};
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int d=72, thick=12, gap=28, startX=20;
                for (int s=0;s<5;s++) {
                    int x=startX+s*(d+gap), count=nn[s];
                    int baseY=getHeight()-24;
                    for (int i=0;i<count;i++) {
                        int cy=baseY-i*thick;
                        // lato scuro (profondità)
                        g2.setColor(cc[s].darker().darker());
                        g2.fillOval(x,cy+thick/2,d,d/3+3);
                        // faccia
                        g2.setColor(cc[s]);
                        g2.fillOval(x,cy,d,d/2);
                        // cerchio interno decorativo
                        g2.setColor(new Color(255,255,255,90));
                        g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{4,3},0));
                        g2.drawOval(x+6,cy+4,d-12,d/2-8);
                        g2.setStroke(new BasicStroke(1f));
                        // valore solo sulla chip in cima
                        if (i==count-1) {
                            g2.setFont(new Font("Arial Black",Font.BOLD,15));
                            g2.setColor(Color.WHITE);
                            FontMetrics fm=g2.getFontMetrics();
                            g2.drawString(ll[s],x+(d-fm.stringWidth(ll[s]))/2,cy+d/4+fm.getAscent()/2);
                        }
                    }
                }
            }
            @Override public Dimension getPreferredSize(){return new Dimension(560,280);}
        };
        chipArea.setOpaque(false);

        JPanel centerContent=new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent,BoxLayout.Y_AXIS));
        centerContent.setOpaque(false);
        centerContent.add(form);
        centerContent.add(Box.createVerticalStrut(22));
        JPanel chipWrap=new JPanel(new FlowLayout(FlowLayout.CENTER,0,0)); chipWrap.setOpaque(false); chipWrap.add(chipArea);
        centerContent.add(chipWrap);

        JPanel center=new JPanel(new GridBagLayout()); center.setOpaque(false); center.add(centerContent);
        root.add(center,BorderLayout.CENTER);
        root.add(footerLabel("I tuoi progressi vengono salvati automaticamente"),BorderLayout.SOUTH);
        return root;
    }
    private void doLogin() {
        String user = loginUserField.getText().trim();
        String pass = new String(loginPassField.getPassword()).trim();
        if (user.isEmpty()||pass.isEmpty()) { setMsg(loginMsgLabel,ERR_COLOR,"Inserisci utente e password."); return; }
        int[] r = leggiUtente(user);
        if (r==null)             { setMsg(loginMsgLabel,ERR_COLOR,"Utente non trovato."); return; }
        if (r[0]!=pass.hashCode()){ setMsg(loginMsgLabel,ERR_COLOR,"Password errata."); return; }
        utenteLoggato=user; saldo=r[1];
        loginMsgLabel.setText(""); loginUserField.setText(""); loginPassField.setText("");
        avviaGioco();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SCHERMATA SIGN UP
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildSignupScreen() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0,0,new Color(10,10,10),getWidth(),getHeight(),FELT_DARK));
                g2.fillRect(0,0,getWidth(),getHeight());
            }
        };
        JLabel logo = new JLabel("♠  BLACKJACK  ♠", SwingConstants.CENTER);
        logo.setFont(LOGO_FONT); logo.setForeground(GOLD_LIGHT);
        logo.setBorder(BorderFactory.createEmptyBorder(50,0,10,0));
        root.add(logo, BorderLayout.NORTH);

        JPanel form = buildAuthForm();
        form.add(centeredLabel("SIGN UP", new Font("Georgia",Font.BOLD,22), GOLD_LIGHT), 0);
        form.add(Box.createVerticalStrut(20), 1);

        signupUserField  = styledTextField();
        signupPassField  = styledPassField();
        signupPass2Field = styledPassField();
        signupMsgLabel   = msgLabel();

        form.add(fieldRow("Utente:",   signupUserField));
        form.add(Box.createVerticalStrut(10));
        form.add(fieldRow("Password:", signupPassField));
        form.add(Box.createVerticalStrut(10));
        form.add(fieldRow("Conferma:", signupPass2Field));
        form.add(Box.createVerticalStrut(14));
        form.add(signupMsgLabel);
        form.add(Box.createVerticalStrut(8));

        JButton btn = actionBtn("CREA ACCOUNT", new Color(18,130,18), Color.WHITE);
        btn.addActionListener(e -> doSignup());
        signupPass2Field.addActionListener(e -> doSignup());
        form.add(wrap(btn));
        form.add(Box.createVerticalStrut(14));

        JLabel link = hyperLabel("Hai già un account? LOG IN");
        link.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { clearLogin(); cards.show(cardLayout,"LOGIN"); }
        });
        form.add(link);

        JPanel center = new JPanel(new GridBagLayout()); center.setOpaque(false); center.add(form);
        root.add(center, BorderLayout.CENTER);
        root.add(footerLabel("Partirai con 1000 € di fiches"), BorderLayout.SOUTH);
        return root;
    }

    private void doSignup() {
        String user=signupUserField.getText().trim(), pass=new String(signupPassField.getPassword()).trim(),
                pass2=new String(signupPass2Field.getPassword()).trim();
        if (user.isEmpty()||pass.isEmpty())    { setMsg(signupMsgLabel,ERR_COLOR,"Compila tutti i campi."); return; }
        if (user.contains(",")||user.contains("\n")){ setMsg(signupMsgLabel,ERR_COLOR,"Nome non valido."); return; }
        if (!pass.equals(pass2))               { setMsg(signupMsgLabel,ERR_COLOR,"Le password non coincidono."); return; }
        if (leggiUtente(user)!=null)           { setMsg(signupMsgLabel,ERR_COLOR,"Nome già in uso."); return; }
        scriviUtente(user, pass.hashCode(), 1000);
        setMsg(signupMsgLabel, OK_COLOR, "Account creato! Puoi fare log in.");
        clearSignup();
        Timer t = new Timer(1500, ev -> { clearLogin(); cards.show(cardLayout,"LOGIN"); });
        t.setRepeats(false); t.start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FILE UTENTI
    // ══════════════════════════════════════════════════════════════════════════
    private int[] leggiUtente(String username) {
        File f = new File(USERS_FILE); if (!f.exists()) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line; while ((line=br.readLine())!=null) {
                String[] p=line.split(",");
                if (p.length==3&&p[0].equals(username)) return new int[]{Integer.parseInt(p[1]),Integer.parseInt(p[2])};
            }
        } catch (Exception ignored) {}
        return null;
    }
    private void scriviUtente(String username, int hashPass, int nuovoSaldo) {
        File f = new File(USERS_FILE); ArrayList<String> righe = new ArrayList<>(); boolean trovato=false;
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line; while ((line=br.readLine())!=null) {
                    String[] p=line.split(",");
                    if (p.length==3&&p[0].equals(username)) { righe.add(username+","+hashPass+","+nuovoSaldo); trovato=true; }
                    else righe.add(line);
                }
            } catch (Exception ignored) {}
        }
        if (!trovato) righe.add(username+","+hashPass+","+nuovoSaldo);
        try (PrintWriter pw=new PrintWriter(new FileWriter(f,false))) { for (String r:righe) pw.println(r); } catch (Exception ignored) {}
    }
    private void aggiornaSaldo() {
        if (utenteLoggato==null) return;
        int[] d=leggiUtente(utenteLoggato); if (d!=null) scriviUtente(utenteLoggato,d[0],saldo);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SCHERMATA DI GIOCO
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildGameScreen() {
        // Wrapper con BorderLayout: tavolo in CENTER, controlli in SOUTH
        JPanel wrapper = new JPanel(new BorderLayout());

        // Il tavolo usa un JLayeredPane per le animazioni
        gameLayered = new JLayeredPane();
        gameLayered.setLayout(null); // layout assoluto per le carte animate

        tablePanel = new JPanel(new BorderLayout(0,10)) {
            @Override protected void paintComponent(Graphics g) { super.paintComponent(g); drawFelt(g); }
        };
        tablePanel.setOpaque(false);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(18,28,10,28));

        // Forza tablePanel a riempire il layeredPane
        tablePanel.setBounds(0,0,1150,600);
        gameLayered.add(tablePanel, JLayeredPane.DEFAULT_LAYER);

        // Resize listener per mantenere tablePanel a tutto il layeredPane
        gameLayered.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                tablePanel.setBounds(0,0,gameLayered.getWidth(),gameLayered.getHeight());
            }
        });

        // Banner + welcome row
        JPanel topArea = new JPanel(new BorderLayout()); topArea.setOpaque(false);
        welcomeLabel = new JLabel("", SwingConstants.RIGHT);
        welcomeLabel.setFont(new Font("Georgia",Font.ITALIC,13)); welcomeLabel.setForeground(new Color(160,160,160));
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(0,0,4,4));
        JButton btnLogout = buildLogoutBtn();
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0)); topRight.setOpaque(false);
        topRight.add(welcomeLabel); topRight.add(btnLogout);
        topArea.add(buildBanner(), BorderLayout.CENTER); topArea.add(topRight, BorderLayout.EAST);
        tablePanel.add(topArea, BorderLayout.NORTH);

        // Zone carte + mazzo
        JPanel center = new JPanel(new BorderLayout(10,0)); center.setOpaque(false);
        JPanel zones  = new JPanel(new GridLayout(2,1,0,16)); zones.setOpaque(false);

        JPanel dealerZone = buildCardZone("DEALER");
        dealerCardPanel  = (JPanel)dealerZone.getClientProperty("cards");
        dealerScoreLabel = (JLabel)dealerZone.getClientProperty("score");

        JPanel playerZone = buildCardZone("GIOCATORE");
        playerCardPanel  = (JPanel)playerZone.getClientProperty("cards");
        playerScoreLabel = (JLabel)playerZone.getClientProperty("score");
        playerZoneLabel  = (JLabel)playerZone.getClientProperty("title");

        zones.add(dealerZone); zones.add(playerZone);
        center.add(zones, BorderLayout.CENTER);
        center.add(buildDeckPanel(), BorderLayout.EAST);
        tablePanel.add(center, BorderLayout.CENTER);

        wrapper.add(gameLayered,       BorderLayout.CENTER);
        wrapper.add(buildControlPanel(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JButton buildLogoutBtn() {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(60,60,60)); g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(new Color(180,180,180)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g2.setFont(new Font("Georgia",Font.PLAIN,11)); g2.setColor(Color.LIGHT_GRAY);
                FontMetrics fm=g2.getFontMetrics(); g2.drawString("Logout",(getWidth()-fm.stringWidth("Logout"))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        b.setPreferredSize(new Dimension(72,26)); b.setBorderPainted(false); b.setContentAreaFilled(false);
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> doLogout()); return b;
    }

    private void doLogout() {
        if (inMano) {
            int s=JOptionPane.showConfirmDialog(this,"Hai una mano in corso. Vuoi uscire? (la puntata sarà persa)","Logout",JOptionPane.YES_NO_OPTION);
            if (s!=JOptionPane.YES_OPTION) return;
            saldo-=puntata;
        }
        aggiornaSaldo(); utenteLoggato=null; inMano=false; clearLogin(); cards.show(cardLayout,"LOGIN");
    }

    // ── Tavolo ────────────────────────────────────────────────────────────────
    private void drawFelt(Graphics g) {
        Graphics2D g2=(Graphics2D)g; int w=tablePanel.getWidth(),h=tablePanel.getHeight();
        g2.setPaint(new GradientPaint(0,0,FELT_MID,w,h,FELT_DARK)); g2.fillRect(0,0,w,h);
        g2.setColor(GOLD); g2.setStroke(new BasicStroke(3f)); g2.drawRoundRect(14,14,w-28,h-28,60,60);
        g2.setColor(new Color(212,175,55,70));
        g2.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{8,5},0));
        g2.drawLine(55,h/2-8,w-55,h/2-8);
    }

    private JPanel buildBanner() {
        JPanel b=new JPanel(new GridLayout(3,1,0,1)); b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(0,0,6,0));
        b.add(goldLbl("BLACKJACK PAYS 3 TO 2",14)); b.add(goldLbl("DEALER MUST HIT SOFT 17",12)); b.add(goldLbl("INSURANCE PAYS 2 TO 1",12));
        return b;
    }
    private JLabel goldLbl(String t,int sz){ JLabel l=new JLabel(t,SwingConstants.CENTER); l.setFont(new Font("Georgia",Font.BOLD,sz)); l.setForeground(GOLD_LIGHT); return l; }

    private JPanel buildCardZone(String title) {
        JPanel zone=new JPanel(new BorderLayout(4,4)); zone.setOpaque(false);
        JLabel lTitle=new JLabel(title); lTitle.setFont(TITLE_FONT); lTitle.setForeground(GOLD); zone.add(lTitle,BorderLayout.NORTH);
        JPanel cp=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); cp.setOpaque(false); zone.add(cp,BorderLayout.CENTER);
        JLabel score=new JLabel("\u2014"); score.setFont(SCORE_FONT); score.setForeground(Color.WHITE); zone.add(score,BorderLayout.SOUTH);
        zone.putClientProperty("cards",cp); zone.putClientProperty("score",score); zone.putClientProperty("title",lTitle);
        return zone;
    }

    private JPanel buildDeckPanel() {
        JPanel deck=new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g); Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int cx=getWidth()/2, cy=getHeight()/2, x0=cx-CW/2, y0=cy-CH/2;
                for (int i=10;i>=1;i--) {
                    g2.setColor(new Color(15,40,100)); g2.fillRoundRect(x0+i,y0-i,CW,CH,8,8);
                    g2.setColor(new Color(25,55,140)); g2.fillRoundRect(x0+i-1,y0-i-1,CW-1,CH-1,8,8);
                }
                ImageIcon bk=caricaBack(); g2.drawImage(bk.getImage(),x0,y0,CW,CH,null);
                g2.setColor(new Color(212,175,55,160)); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(x0,y0,CW-1,CH-1,8,8);
                g2.setFont(new Font("Georgia",Font.BOLD,11)); g2.setColor(new Color(212,175,55,180));
                FontMetrics fm=g2.getFontMetrics(); String txt="MAZZO";
                g2.drawString(txt,cx-fm.stringWidth(txt)/2,y0+CH+18);

                // Aggiorna posizione schermo del mazzo per le animazioni
                Point p=SwingUtilities.convertPoint(this,x0,y0,gameLayered);
                deckScreenPos=p;
            }
            @Override public Dimension getPreferredSize(){ return new Dimension(130,0); }
        };
        deck.setOpaque(false); return deck;
    }

    // ── Pannello controlli IN BASSO (più grande) ──────────────────────────────
    private JPanel buildControlPanel() {
        JPanel outer=new JPanel(new BorderLayout());
        outer.setBackground(new Color(18,18,18));
        outer.setBorder(BorderFactory.createMatteBorder(3,0,0,0,GOLD));

        messageLabel=new JLabel("Scegli la puntata con le chip e premi DEAL.", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Georgia",Font.BOLD,15));
        messageLabel.setForeground(GOLD_LIGHT);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(10,10,6,10));
        outer.add(messageLabel, BorderLayout.NORTH);

        // Riga centrale: chip + puntata/saldo
        JPanel midRow=new JPanel(new BorderLayout(16,0));
        midRow.setOpaque(false); midRow.setBorder(BorderFactory.createEmptyBorder(8,24,8,24));

        // Chip più grandi
        chipPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); chipPanel.setOpaque(false);
        for (int i=0;i<CHIP_VALUES.length;i++) chipPanel.add(buildChip(CHIP_VALUES[i],CHIP_COLORS[i]));
        midRow.add(chipPanel, BorderLayout.WEST);

        // Puntata + saldo
        JPanel infoBox=new JPanel(new GridLayout(2,1,0,6)); infoBox.setOpaque(false);
        puntataField=new JTextField("0");
        puntataField.setFont(new Font("Courier New",Font.BOLD,22));
        puntataField.setForeground(Color.WHITE); puntataField.setBackground(new Color(38,38,38));
        puntataField.setHorizontalAlignment(JTextField.CENTER);
        puntataField.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(GOLD),"PUNTATA",
                TitledBorder.LEFT,TitledBorder.TOP,new Font("Georgia",Font.PLAIN,11),GOLD));
        puntataField.setPreferredSize(new Dimension(200,55));

        saldoLabel=new JLabel("SALDO: "+saldo+" \u20ac",SwingConstants.CENTER);
        saldoLabel.setFont(new Font("Georgia",Font.BOLD,18)); saldoLabel.setForeground(new Color(100,240,100));

        infoBox.add(puntataField); infoBox.add(saldoLabel);
        midRow.add(infoBox, BorderLayout.CENTER);
        outer.add(midRow, BorderLayout.CENTER);
        outer.add(buildButtonRow(), BorderLayout.SOUTH);
        return outer;
    }

    private JButton buildChip(int value, Color color) {
        JButton btn=new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Color c=isEnabled()?color:color.darker().darker();
                g2.setColor(c); g2.fillOval(2,2,getWidth()-4,getHeight()-4);
                g2.setColor(isEnabled()?Color.WHITE:Color.GRAY);
                g2.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{5,3},0));
                g2.drawOval(5,5,getWidth()-10,getHeight()-10);
                // cerchio interno decorativo
                g2.setStroke(new BasicStroke(1f)); g2.drawOval(9,9,getWidth()-18,getHeight()-18);
                g2.setFont(new Font("Arial Black",Font.BOLD,12));
                g2.setColor(isEnabled()?Color.WHITE:Color.GRAY);
                FontMetrics fm=g2.getFontMetrics(); String t=String.valueOf(value);
                g2.drawString(t,(getWidth()-fm.stringWidth(t))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btn.setPreferredSize(new Dimension(64,64));
        btn.setBorderPainted(false); btn.setContentAreaFilled(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); btn.setToolTipText("+"+value+" \u20ac");
        btn.addActionListener(e->{ if(!inMano){ puntata+=value; puntataField.setText(String.valueOf(puntata)); }});
        return btn;
    }

    private JPanel buildButtonRow() {
        JPanel row=new JPanel(new FlowLayout(FlowLayout.CENTER,22,10)); row.setOpaque(false);
        btnReset=styledBtn("RESET",new Color(100,100,100),Color.WHITE);
        btnReset.addActionListener(e->{ if(!inMano){ puntata=0; puntataField.setText("0"); }});
        btnDeal=styledBtn("DEAL",GOLD,new Color(25,25,25));
        btnDeal.addActionListener(e->iniziaMano());
        btnHit=styledBtn("HIT",new Color(175,28,28),Color.WHITE);
        btnHit.addActionListener(e->playerHit());
        btnStand=styledBtn("STAND",new Color(18,130,18),Color.WHITE);
        btnStand.addActionListener(e->playerStand());
        row.add(btnReset); row.add(btnDeal); row.add(btnHit); row.add(btnStand);
        return row;
    }

    private JButton styledBtn(String text,Color bg,Color fg) {
        JButton btn=new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Color base=isEnabled()?bg:new Color(70,70,70);
                g2.setPaint(new GradientPaint(0,0,base.brighter(),0,getHeight(),base.darker()));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),30,30);
                g2.setColor(isEnabled()?fg.darker():Color.DARK_GRAY); g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,30,30);
                g2.setFont(BTN_FONT); g2.setColor(isEnabled()?fg:Color.GRAY);
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(text,(getWidth()-fm.stringWidth(text))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btn.setPreferredSize(new Dimension(140,54));
        btn.setBorderPainted(false); btn.setContentAreaFilled(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ANIMAZIONE CARTE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Anima una carta dal mazzo verso la destinazione target.
     * useBack=true → mostra il dorso; onDone viene chiamato alla fine.
     */
    private void animaCarta(ImageIcon icon, Point target, boolean useBack, Runnable onDone) {
        ImageIcon showIcon = useBack ? caricaBack() : icon;
        JLabel animLabel = new JLabel(scaleIcon(showIcon, CW, CH));
        animLabel.setSize(CW, CH);
        animLabel.setLocation(deckScreenPos);
        gameLayered.add(animLabel, JLayeredPane.DRAG_LAYER);
        gameLayered.repaint();

        int startX = deckScreenPos.x, startY = deckScreenPos.y;
        int endX = target.x, endY = target.y;
        int STEPS = 18;
        int[] step = {0};

        Timer t = new Timer(16, null);
        t.addActionListener(e -> {
            step[0]++;
            float prog = (float)step[0] / STEPS;
            // Ease out
            float ease = 1f - (1f-prog)*(1f-prog);
            int cx2 = startX + (int)((endX-startX)*ease);
            int cy2 = startY + (int)((endY-startY)*ease);
            animLabel.setLocation(cx2, cy2);
            gameLayered.repaint();
            if (step[0] >= STEPS) {
                t.stop();
                gameLayered.remove(animLabel);
                gameLayered.repaint();
                if (onDone != null) SwingUtilities.invokeLater(onDone);
            }
        });
        t.start();
    }

    /**
     * Anima una sequenza di carte una dopo l'altra.
     * Ogni elemento di 'targets' corrisponde a una carta: {icon, targetPoint, useBack}.
     */
    private void animaSequenza(ArrayList<Object[]> sequenza, Runnable onAllDone) {
        if (sequenza.isEmpty()) { if (onAllDone!=null) SwingUtilities.invokeLater(onAllDone); return; }
        Object[] item = sequenza.remove(0);
        ImageIcon icon   = (ImageIcon) item[0];
        Point     target = (Point)     item[1];
        boolean   back   = (boolean)   item[2];
        animaCarta(icon, target, back, () -> animaSequenza(sequenza, onAllDone));
    }

    /** Calcola la posizione in gameLayered della prossima carta da aggiungere a un pannello carte */
    private Point targetPos(JPanel cardPanel, int existingCount) {
        // Posizione del pannello nel gameLayered
        Point panelPos = SwingUtilities.convertPoint(cardPanel, 0, 0, gameLayered);
        int x = panelPos.x + existingCount * (CW + 8);
        int y = panelPos.y;
        return new Point(x, y);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGICA DI GIOCO
    // ══════════════════════════════════════════════════════════════════════════
    private int[] pescaCarta() { return new int[]{rnd.nextInt(13), rnd.nextInt(4)}; }
    private int valoreCarta(int[] c) { if(c[0]==0) return 11; if(c[0]>=10) return 10; return c[0]+1; }
    private int sommaMano(ArrayList<int[]> m) {
        int tot=0, assi=0;
        for(int[]c:m){ tot+=valoreCarta(c); if(c[0]==0) assi++; }
        while(tot>21 && assi>0){ tot-=10; assi--; }
        return tot;
    }

    private void avviaGioco() {
        manoGiocatore.clear(); manoDealer.clear();
        cartaNascostaVisibile=false; inMano=false; puntata=0; puntataField.setText("0");
        playerZoneLabel.setText(utenteLoggato.toUpperCase());
        welcomeLabel.setText("Ciao, "+utenteLoggato+"!");
        saldoLabel.setText("SALDO: "+saldo+" \u20ac");
        messageLabel.setText("Scegli la puntata con le chip e premi DEAL.");
        aggiornaTastiInizio(); refreshCards();
        cards.show(cardLayout,"GAME");
    }

    private void iniziaMano() {
        puntata=parseIntSafe(puntataField.getText());
        if (puntata<=0||puntata>saldo) { messageLabel.setText("\u26a0  Puntata non valida (1\u2013"+saldo+" \u20ac)"); return; }
        manoGiocatore.clear(); manoDealer.clear();
        cartaNascostaVisibile=false; inMano=true;

        final int[] c1g=pescaCarta(), c2g=pescaCarta(), c1d=pescaCarta(), c2d=pescaCarta();
        aggiornaTastiInMano();
        messageLabel.setText("Distribuzione in corso...");

        // Step 4: c2d dealer (coperta) arriva -> fine distribuzione
        Runnable step4 = () -> {
            manoDealer.add(c2d);
            refreshCards();
            int sg = sommaMano(manoGiocatore);
            messageLabel.setText("Carte: "+valoreCarta(c1g)+" + "+valoreCarta(c2g)+" = "+sg
                    +"   |   Dealer mostra: "+valoreCarta(c1d));
            if (sg==21) SwingUtilities.invokeLater(BlackjackGUI.this::playerStand);
        };

        // Step 3: c2g giocatore arriva -> anima c2d dealer coperta
        Runnable step3 = () -> {
            manoGiocatore.add(c2g);
            refreshCards();
            Point posD1 = targetPos(dealerCardPanel, manoDealer.size());
            animaCarta(caricaCarta(c2d), posD1, true, step4);
        };

        // Step 2: c1d dealer arriva -> anima c2g giocatore
        Runnable step2 = () -> {
            manoDealer.add(c1d);
            refreshCards();
            Point posP1 = targetPos(playerCardPanel, manoGiocatore.size());
            animaCarta(caricaCarta(c2g), posP1, false, step3);
        };

        // Step 1: c1g giocatore arriva -> anima c1d dealer
        Runnable step1 = () -> {
            manoGiocatore.add(c1g);
            refreshCards();
            Point posD0 = targetPos(dealerCardPanel, manoDealer.size());
            animaCarta(caricaCarta(c1d), posD0, false, step2);
        };

        // Avvia sequenza
        Point posP0 = targetPos(playerCardPanel, 0);
        animaCarta(caricaCarta(c1g), posP0, false, step1);
    }

    private void playerHit() {
        int[] nuova=pescaCarta();
        int esistenti=manoGiocatore.size();
        Point target=targetPos(playerCardPanel, esistenti);
        animaCarta(caricaCarta(nuova), target, false, () -> {
            manoGiocatore.add(nuova);
            int sg=sommaMano(manoGiocatore);
            refreshCards();
            messageLabel.setText("Hai pescato: "+valoreCarta(nuova)+"  \u2014  Totale: "+sg);
            if (sg>21) { cartaNascostaVisibile=true; refreshCards(); fineRound("Hai sballato! Il dealer vince.",-puntata); }
        });
    }

    private void playerStand() {
        cartaNascostaVisibile=true; refreshCards();
        // Dealer pesca con animazioni sequenziali
        dealerPesca();
    }

    private void dealerPesca() {
        int sd=sommaMano(manoDealer);
        if (sd>17 || sd>21) { concludiStand(); return; }
        int[] nuova=pescaCarta();
        int esistenti=manoDealer.size();
        Point target=targetPos(dealerCardPanel, esistenti);
        animaCarta(caricaCarta(nuova), target, false, () -> {
            manoDealer.add(nuova);
            refreshCards();
            dealerPesca(); // ricorsione finché il dealer non si ferma
        });
    }

    private void concludiStand() {
        int sg=sommaMano(manoGiocatore), sd=sommaMano(manoDealer);
        String msg; int delta;
        if      (sg==21&&manoGiocatore.size()==2) { delta=(int)(puntata*1.5); msg="BLACKJACK! +"+delta+" \u20ac"; }
        else if (sd>21||sg>sd)                    { delta=puntata;            msg="Hai vinto! +"+delta+" \u20ac"; }
        else if (sg<sd)                           { delta=-puntata;           msg="Il dealer vince. "+delta+" \u20ac"; }
        else                                      { delta=0;                  msg="Pareggio!"; }
        fineRound(msg, delta);
    }

    private void fineRound(String msg, int delta) {
        saldo+=delta; inMano=false; puntata=0; puntataField.setText("0");
        messageLabel.setText(msg); saldoLabel.setText("SALDO: "+saldo+" \u20ac");
        aggiornaSaldo(); refreshCards();
        if (saldo<=0) { messageLabel.setText("Hai finito le fiches! Partita terminata."); aggiornaTastiFinito(); }
        else          { aggiornaTastiInizio(); }
    }

    // ── Stato tasti ───────────────────────────────────────────────────────────
    private void aggiornaTastiInizio() {
        btnDeal.setEnabled(true); btnHit.setEnabled(false); btnStand.setEnabled(false); btnReset.setEnabled(true);
        setChipEnabled(true); repaintButtons();
    }
    private void aggiornaTastiInMano() {
        btnDeal.setEnabled(false); btnHit.setEnabled(true); btnStand.setEnabled(true); btnReset.setEnabled(false);
        setChipEnabled(false); repaintButtons();
    }
    private void aggiornaTastiFinito() {
        btnDeal.setEnabled(false); btnHit.setEnabled(false); btnStand.setEnabled(false); btnReset.setEnabled(false);
        setChipEnabled(false); repaintButtons();
    }
    private void setChipEnabled(boolean en) { for(Component c:chipPanel.getComponents()){c.setEnabled(en);c.repaint();} }
    private void repaintButtons() { btnDeal.repaint(); btnHit.repaint(); btnStand.repaint(); btnReset.repaint(); }

    // ── Rendering carte (statico) ─────────────────────────────────────────────
    private void refreshCards() {
        dealerCardPanel.removeAll();
        for (int i=0;i<manoDealer.size();i++) {
            ImageIcon ico=(i==1&&!cartaNascostaVisibile)?caricaBack():caricaCarta(manoDealer.get(i));
            dealerCardPanel.add(new JLabel(scaleIcon(ico,CW,CH)));
        }
        if (manoDealer.isEmpty()) dealerScoreLabel.setText("\u2014");
        else if (!cartaNascostaVisibile) dealerScoreLabel.setText(String.valueOf(valoreCarta(manoDealer.get(0))));
        else dealerScoreLabel.setText(String.valueOf(sommaMano(manoDealer)));

        playerCardPanel.removeAll();
        for (int[]c:manoGiocatore) playerCardPanel.add(new JLabel(scaleIcon(caricaCarta(c),CW,CH)));
        playerScoreLabel.setText(manoGiocatore.isEmpty()?"\u2014":String.valueOf(sommaMano(manoGiocatore)));

        saldoLabel.setText("SALDO: "+saldo+" \u20ac");
        dealerCardPanel.revalidate(); dealerCardPanel.repaint();
        playerCardPanel.revalidate(); playerCardPanel.repaint();
        tablePanel.repaint();
    }

    private ImageIcon scaleIcon(ImageIcon icon, int w, int h) {
        Image scaled=icon.getImage().getScaledInstance(w,h,Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private ImageIcon caricaCarta(int[] carta) {
        String nome=VALORI[carta[0]]+"_of_"+SEMI[carta[1]]+".png";
        File f=new File(CARD_PATH+nome);
        if (f.exists()) { try { return new ImageIcon(ImageIO.read(f)); } catch (Exception ignored) {} }
        return placeholder(VALORI[carta[0]].toUpperCase(),SEMI[carta[1]],false);
    }
    private ImageIcon caricaBack() {
        for (String n:new String[]{"card_back.png","back.png","back_of_card.png","Card_Back.png"}) {
            File f=new File(CARD_PATH+n); if (f.exists()) { try { return new ImageIcon(ImageIO.read(f)); } catch(Exception ignored){} }
        }
        return placeholder("?","back",true);
    }
    private ImageIcon placeholder(String val,String seme,boolean isBack) {
        BufferedImage img=new BufferedImage(CW,CH,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2=img.createGraphics(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isBack?new Color(30,60,150):Color.WHITE); g2.fillRoundRect(0,0,CW,CH,10,10);
        g2.setColor(new Color(160,160,160)); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(1,1,CW-2,CH-2,10,10);
        if (!isBack) {
            boolean red=seme.equals("hearts")||seme.equals("diamonds");
            g2.setColor(red?new Color(180,20,20):new Color(20,20,20));
            g2.setFont(new Font("Georgia",Font.BOLD,20)); FontMetrics fm=g2.getFontMetrics();
            g2.drawString(val,(CW-fm.stringWidth(val))/2,CH/2+6);
        } else {
            g2.setColor(new Color(100,140,220)); g2.setFont(new Font("Arial",Font.BOLD,32)); g2.drawString("\u2660",CW/2-12,CH/2+10);
        }
        g2.dispose(); return new ImageIcon(img);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPER UI
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildAuthForm() {
        JPanel form=new JPanel(); form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));
        form.setBackground(PANEL_BG);
        form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(GOLD,2),BorderFactory.createEmptyBorder(30,40,30,40)));
        form.setMaximumSize(new Dimension(400,440)); return form;
    }
    private JTextField styledTextField() {
        JTextField tf=new JTextField(18); tf.setFont(INPUT_FONT); tf.setForeground(Color.WHITE);
        tf.setBackground(INPUT_BG); tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(70,70,70)),BorderFactory.createEmptyBorder(7,10,7,10)));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE,40)); return tf;
    }
    private JPasswordField styledPassField() {
        JPasswordField pf=new JPasswordField(18); pf.setFont(INPUT_FONT); pf.setForeground(Color.WHITE);
        pf.setBackground(INPUT_BG); pf.setCaretColor(Color.WHITE);
        pf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(70,70,70)),BorderFactory.createEmptyBorder(7,10,7,10)));
        pf.setMaximumSize(new Dimension(Integer.MAX_VALUE,40)); return pf;
    }
    private JPanel fieldRow(String labelText,JComponent field) {
        JPanel row=new JPanel(new BorderLayout(8,0)); row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        JLabel lbl=new JLabel(labelText); lbl.setFont(LABEL_FONT); lbl.setForeground(new Color(180,180,180)); lbl.setPreferredSize(new Dimension(72,20));
        row.add(lbl,BorderLayout.WEST); row.add(field,BorderLayout.CENTER); return row;
    }
    private JLabel centeredLabel(String text,Font font,Color color) {
        JLabel l=new JLabel(text,SwingConstants.CENTER); l.setFont(font); l.setForeground(color); l.setAlignmentX(Component.CENTER_ALIGNMENT); return l;
    }
    private JLabel msgLabel() {
        JLabel l=new JLabel(" ",SwingConstants.CENTER); l.setFont(new Font("Georgia",Font.PLAIN,12)); l.setForeground(ERR_COLOR); l.setAlignmentX(Component.CENTER_ALIGNMENT); return l;
    }
    private JLabel hyperLabel(String text) {
        JLabel l=new JLabel(text,SwingConstants.CENTER); l.setFont(new Font("Georgia",Font.PLAIN,12)); l.setForeground(new Color(140,140,255)); l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); l.setAlignmentX(Component.CENTER_ALIGNMENT); return l;
    }
    private JLabel footerLabel(String text) {
        JLabel l=new JLabel(text,SwingConstants.CENTER); l.setFont(new Font("Georgia",Font.ITALIC,11)); l.setForeground(new Color(120,120,120)); l.setBorder(BorderFactory.createEmptyBorder(0,0,20,0)); return l;
    }
    private JButton actionBtn(String text,Color bg,Color fg) {
        JButton btn=new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0,0,bg.brighter(),0,getHeight(),bg.darker())); g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
                g2.setFont(BTN_FONT); g2.setColor(fg); FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btn.setPreferredSize(new Dimension(210,42)); btn.setMaximumSize(new Dimension(210,42));
        btn.setBorderPainted(false); btn.setContentAreaFilled(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); btn.setAlignmentX(Component.CENTER_ALIGNMENT); return btn;
    }
    private JPanel wrap(JComponent c) { JPanel p=new JPanel(new FlowLayout(FlowLayout.CENTER,0,0)); p.setOpaque(false); p.add(c); return p; }
    private void setMsg(JLabel l,Color c,String t) { l.setForeground(c); l.setText(t); }
    private void clearLogin()  { loginUserField.setText(""); loginPassField.setText(""); loginMsgLabel.setText(" "); }
    private void clearSignup() { signupUserField.setText(""); signupPassField.setText(""); signupPass2Field.setText(""); signupMsgLabel.setText(" "); }
    private int parseIntSafe(String s) { try{return Integer.parseInt(s.trim());}catch(NumberFormatException e){return 0;} }

    public static void main(String[] args) {
        try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){}
        SwingUtilities.invokeLater(Main::new);
    }
}