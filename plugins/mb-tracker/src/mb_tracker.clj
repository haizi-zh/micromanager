(ns mb-tracker
  (:import (ij IJ ImagePlus WindowManager ImageListener)
           (ij.measure Measurements)
           (ij.gui ImageCanvas) 
           (javax.swing SwingUtilities, KeyStroke, JComponent, Box, JFrame, JLabel, JPanel,
                        JTabbedPane, JToolBar, JScrollPane, JButton, JList, JTextField,
                        JTextArea, JFileChooser, BoxLayout, BorderFactory, JOptionPane,
                        JSlider)
           (javax.swing.event ChangeListener)
           (java.awt Dimension, BorderLayout, FlowLayout, GridBagLayout, GridBagConstraints, 
                     Insets, Rectangle)
           (java.awt.event ActionListener
                           WindowListener
                           WindowAdapter
                           ComponentListener
                           ComponentAdapter
                           ActionEvent
                           KeyEvent)))

(gen-class
  :name "org.zephyre.MBTracker"
  :implements ["java.lang.Runnable"])

; UI components
(def components (atom {}))
(def init-image (atom nil))

(defn init-image-listener [] "Image listener for displaying the location crosshair."
  (let [image-updated (fn [image] ())]
    (reify ImageListener
      (imageClosed [this image] 
        (IJ/log "Image closed.")
        (.dispose (:main-frame @components)))
      (imageOpened [this image] (IJ/log (format "%s%s" "Image opened: " (.getTitle image))))
      (imageUpdated [this image]
        (if (identical? image @init-image) 
          (IJ/log (format "%s%d" "Image updated: " (.getCurrentSlice image)))))))) 

(def image-listener (atom (init-image-listener)))

(defn register-listener [] "Register listeners for image open/updated/close."
  (ImagePlus/addImageListener @image-listener)
  (IJ/log "ImagePlus/addImageListener"))    

(defn unregister-listener []
  (ImagePlus/removeImageListener @image-listener)
  (IJ/log "ImagePlus/removeImageListener")) 

(defn init-status-bar []
  (let [bar (JToolBar.), label (JLabel. "Idle.")]
    (.add bar label)
    (swap! components assoc :status-label label)
    (identity bar)))

(def atom-thrd (atom nil))
(def atom-image (atom nil))

(defn update-canvas [thrd] "Update the canvas according to new threshold values."
  (let [pixels (.getPixels (.getProcessor @atom-image))]
    (dorun (map (fn [idx]
                  (aset ^shorts pixels idx (short thrd)))
;                  (if (< (aget ^shorts pixels idx) thrd) (aset ^shorts pixels idx (short 0))))
                (range (count thrd))))
    (.updateAndRepaintWindow @atom-image)
    (.repaint (:main-frame @components))))

(defn perform-tracking [image roi thrd])

(defn on-ok-btn [thrd]
  "The OK button is pressed"
  (let [image (WindowManager/getCurrentImage)]
    (if (not image) (IJ/showMessage "Error" "No image is present.")
      (let [roi (try (.getBounds (.getRoi image))
                  (catch NullPointerException e
                    (Rectangle. (.getWidth image) (.getHeight image))))
            data (.getPixels (.getProcessor image))
            width (.getWidth image)
            height (.getHeight image)
            ; [1 2 3 ... 1 2 3 ... ...]
            xseq (apply concat (repeat height (range width)))
            ; [1 1 1 ... 2 2 2 ... 3 3 3 ... ... n n n ...]
            yseq (apply concat (map #(repeat width %) (range height)))]
        (/ (reduce + (map * data xseq)) (reduce + data))))))

(defn init-ctrl-panel [thrd-range]
  (let [panel (JPanel.)
        field (JTextField. (first thrd-range))
        btn (JButton. "Go!")
        image (WindowManager/getCurrentImage)]
;        slider (JSlider. (int (first thrd-range)) (int (last thrd-range)))]
    (doto field
      (.setText "32")
      (.setPreferredSize (Dimension. 32 (.height (.getPreferredSize field)))))
    (.requestFocus btn)
    (.addActionListener btn
      (reify ActionListener
        (actionPerformed [this evt]
          (try 
            (on-ok-btn (Integer/parseInt (.getText field)))
            (catch NumberFormatException e (IJ/showMessage "Error" "Invalid threshold."))))))
    (doto panel
;      (.setBorder panel (BorderFactory/createEmptyBorder 4 4 4 4))
      (.setLayout (FlowLayout.))
      (.add (JLabel. "Threshold:"))
      (.add field)
      (.add btn))))

(defn init-main-panel
  [frame thrd-range]
  (let [panel (JPanel.)
        layout (BorderLayout.)]
    (doto layout
      (.setHgap 8)
      (.setVgap 8))
    (doto panel
      (.setLayout layout)
      (.add (init-ctrl-panel thrd-range) (BorderLayout/NORTH))
      (.add (init-status-bar) (BorderLayout/SOUTH))
      (.setBorder (BorderFactory/createEmptyBorder 4 4 4 4)))
    (let [image (WindowManager/getCurrentImage)]
      (if image        
        (let [img (ImagePlus. "Threshold" (.duplicate (.getProcessor image)))
              canvas (ImageCanvas. img)]
          (.add panel canvas)
          (reset! atom-image img))))
    panel))

(defn init-ui [thrd-range] "Initialize the main frame"
  (let [frame (JFrame. "Magnetic Beads Tracker v0.9 / Zephyre")]
    (.add (.getContentPane frame) (init-main-panel frame thrd-range))
    (.registerKeyboardAction
      (.getRootPane frame)
      (reify ActionListener (actionPerformed [this evt] (.dispose frame)))
      (KeyStroke/getKeyStroke KeyEvent/VK_ESCAPE 0)
      (JComponent/WHEN_IN_FOCUSED_WINDOW))
    (doto frame
      (.setDefaultCloseOperation (JFrame/DISPOSE_ON_CLOSE))
      (.setLocationRelativeTo nil)
      (.addWindowListener
        (proxy [WindowAdapter] []
          (windowOpened [evt] (register-listener))
          (windowClosed [evt] (unregister-listener)))) 
;      (.addComponentListener
;        (proxy [ComponentAdapter] []
;          (componentShown [evt] (register-listener))
;          (componentHidden [evt] (unregister-listener))))
      (.pack))))

(defn min-max-over-stack [image]
  (let [n (.getNSlices image)
        initSlice (.getSlice image)]
    (.setSlice image 1)
    (let [result (reduce (fn [val idx]
                           (.setSlice image idx)
                           (IJ/showProgress idx n)
                           (let [stat (.getStatistics image)]
                             [(min (first val) (.min stat))
                              (max (last val) (.max stat))]))            
                         (let [stat (.getStatistics image)] [(.min stat) (.max stat)])
                         (range 2 (inc n)))]
      (.setSlice image initSlice)
      (identity result))))

(defn -run [this]
  (let [image (WindowManager/getCurrentImage)]
    (if image
      (do
        (let [new-frame (init-ui (min-max-over-stack image))]
          (swap! components assoc :main-frame new-frame)
          (.setVisible new-frame true))
        (reset! init-image image))
      (let [new-frame (init-ui [0 255])]
          (swap! components assoc :main-frame new-frame)
          (.setVisible new-frame true)))))      