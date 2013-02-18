(ns ccd-gain-calc
  (:import (ij IJ ImagePlus WindowManager ImageListener)
           (ij.measure Measurements)
           (ij.process ImageProcessor ByteProcessor ShortProcessor FloatProcessor)
           (org.apache.commons.math3.stat.descriptive SummaryStatistics) 
           (javax.swing SwingUtilities KeyStroke JComponent Box JFrame JLabel JPanel
                        JTabbedPane JToolBar JScrollPane JButton JList JTextField
                        JTextArea JFileChooser BoxLayout BorderFactory JOptionPane)
           (java.awt Dimension BorderLayout GridBagLayout GridBagConstraints Insets Rectangle)
           (java.awt.event ActionListener WindowListener WindowAdapter ComponentListener ComponentAdapter
                           ActionEvent KeyEvent)))

(gen-class
  :name "org.zephyre.CCDGainCalc"
  :implements ["java.lang.Runnable"])

(def stat1 (atom (SummaryStatistics.)))
(def stat2 (atom (SummaryStatistics.)))

(defn diff-proc-short
  "Process two images by calculating the diff's var and mean."
  [data1 data2 size]
  (let [;The mean of the two images
        [mean1 mean2] (map #(double (/ % size))
                           (map (fn [#^shorts arr] (areduce arr i ret 0 (+ ret (aget arr i))))
                                [data1 data2]))
        ratio (/ mean1 mean2)]
    [mean1 (/ (areduce ^shorts data1 i ret 0 
                       (let [ele (- (aget ^shorts data1 i) (* ratio (aget ^shorts data2 i)))] 
                         (+ ret (* ele ele))))
              size)]))
    
(defn diff-proc-byte
  "Process two images by calculating the diff's var and mean."
  [data1 data2 size]
  (let [;The mean of the two images
        [mean1 mean2] (map #(double (/ % size))
                           (map (fn [#^bytes arr] (areduce arr i ret 0 (+ ret (aget arr i))))
                                [data1 data2]))
        ratio (/ mean1 mean2)]
    [mean1 (/ (areduce ^bytes data1 i ret 0 
                       (let [ele (- (aget ^bytes data1 i) (* ratio (aget ^bytes data2 i)))] 
                         (+ ret (* ele ele)))) 
              size)]))

(defn stack-proc
  "Process the stack. Return: [[mean1 var1] [mean2 var2]...]"
  [stack]
  (let [ip (.getProcessor stack)
        proc-func (condp instance? ip
                    ByteProcessor diff-proc-byte
                    ShortProcessor diff-proc-short)
        func (fn [slice]
               (let [_ (.setSlice stack slice)
                     data1 (.getPixelsCopy ip)
                     _ (.setSlice stack (inc slice))
                     data2 (.getPixelsCopy ip)]
                 (proc-func data1 data2 (count data1))))]
    (map func (range 1 (.getNSlices stack)))))

(defn multi-proc
  "Process a serie of image stacks. Return: [[mean var]...]"
  [images]
  (reduce concat (map stack-proc images)))

(defn open-images
  "Provide a multi-sel dialog to select image files. Return: [files...]"
  [parent]
  (let [chooser (JFileChooser.)]
    (.setMultiSelectionEnabled chooser true)
    (if (= (JFileChooser/APPROVE_OPTION) (.showOpenDialog chooser parent))
      (map #(IJ/openImage (.getAbsolutePath %)) (.getSelectedFiles chooser))
      nil)))
        
(defn -run [this]
  (IJ/log "Plugin Started.")
  (open-images nil))
;  (dorun (stack-proc (WindowManager/getCurrentImage))))