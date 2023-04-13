package org.aiotrade.lib.trading.charting

import com.sun.javafx.stage.EmbeddedWindow
import java.awt.Container
import java.awt.event.ActionListener
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.image.BufferedImage
import java.io.File
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.embed.swing.JFXPanel
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Scene
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.Timer

/**
 *
 * @author Caoyuan Deng
 */
class FXImager(time: Int) {

  /**
   * Some of the important private variables
   */
  private var stage: EmbeddedWindow = _
  private var scene: Scene = _
  private var list: ObservableList[Node] = _
  private var node: Node = _

  /**
   *
   * @param scene the scene which is to be imaged
   * @param save place where the image to be saved
   * @param width width of image
   * @param height height of image
   */
  def sceneToImage(scene: Scene, save: File, width: Double, height: Double) {
    stage = scene.getWindow.asInstanceOf[EmbeddedWindow] // or Stage?
    this.scene = scene
    val bound = if (width > 0 && height > 0) {
      new BoundingBox(0, 0, width, height)
    } else null
    initAndShowGUI(scene, save, bound)
  }

  /**
   * Overload  function of sceneToImage
   *
   * @param scene
   * @param save
   */
  def sceneToImage(scene: Scene, save: File) {
    sceneToImage(scene, save, 0, 0)
  }

  /**
   * This function helps to save the Node to Image
   * and it's the only function which is public
   *
   * @param node node to be saved
   * @param list list of children where the node is kept
   * @param save place where the image to be saved
   */
  def nodeToImage(node: Node, list: ObservableList[Node], save: File) {
    nodeToImage(node, list, save, 0, 0)
  }

  /**
   * This function helps to save the Node to Image
   * and it's the only function which is public
   *
   * @param node the node to be saved
   * @param list the ObservableList of children where the node is kept
   * @param save place where the image to be saved
   * @param width width of image to be saved
   * @param height height of image to be saveed
   */
  def nodeToImage(node: Node, list: ObservableList[Node], save: File, width: Double , height: Double) {
    stage = node.getScene.getWindow.asInstanceOf[EmbeddedWindow] // or Stage?
    scene = node.getScene
    this.node = node
    this.list = list
    //list.remove(node)
    val bound = if (width > 0 && height > 0) new BoundingBox(0, 0, width, height) else null
    initAndShowGUI(node, save, bound)
  }

  /**
   * This is the main function to generate the graphics of the Node
   * using the FXPanel inside the JFrame.
   * @param node
   * @param f
   * @param bound
   */
  private def initAndShowGUI(node: Node, file: File, bound: BoundingBox) {
    val root = new Group()
    val scene = new Scene(root)
    root.getChildren.add(node)
    initAndShowGUI(scene, file, bound)
  }

  /**
   * This is the main function to generate the graphics of the scene
   * using the FXPanel inside the JFrame.
   *
   * @param scene
   * @param file
   * @param bound
   */
  private def initAndShowGUI(scene: Scene, file: File, bound: BoundingBox) {
    val boundbox = if (bound == null) new BoundingBox(0, 0, stage.getWidth, stage.getHeight) else bound

    //Frame.setUndecorated(true)
    val frame = new JFrame()
    val fxPanel = new JFXPanel()
    fxPanel.setScene(scene)

    var cl: ComponentListener = null
    cl = new ComponentListener {
      def componentResized(e: ComponentEvent) {
        // set timer for capturing image of the Node
        var timer: Timer = null
        timer = new Timer(time, new ActionListener {
            def actionPerformed(e: java.awt.event.ActionEvent) {
              save(fxPanel, boundbox, file)
              timer.stop
              timer == null
              fxPanel.removeComponentListener(cl)
              fxPanel.removeAll
              restoreState
              frame.dispose
            }
          })
        
        timer.start
      }
      def componentMoved (e: ComponentEvent) {}
      def componentShown (e: ComponentEvent) {}
      def componentHidden(e: ComponentEvent) {}
    }
    
    fxPanel.addComponentListener(cl)      

    frame.add(fxPanel)
    frame.setSize(boundbox.getWidth.toInt, boundbox.getHeight.toInt)
    if (stage != null) {
      frame.setLocation(stage.getX.toInt, stage.getY.toInt)
      Platform.runLater(new Runnable {
          def run {
            stage.hide
            frame.pack
            frame.setVisible(true)
          }
        })
    }
  }

  /**
   * This function saves the container as FXPanel
   * to the Image using the Java API
   * @param container
   * @param bounds
   * @param file
   */
  private def save(container: Container, bounds: Bounds, file: File)  {
    try {
      val name = file.getName
      val dot = name.lastIndexOf(".")
      val ext = if (dot >= 0) {
        name.substring(dot + 1)
      } else {
        "jpg"
      }
      ImageIO.write(toBufferedImage(container, bounds), ext, file)
      println("Node To Image saved")
    } catch {
      case ex: java.lang.Exception =>
        ex.printStackTrace
        JOptionPane.showMessageDialog(null, "The image couldn't be saved", "Error", JOptionPane.ERROR_MESSAGE)
        restoreState
    }
  }

  private def restoreState {
    if (node != null) {
      restoreNode
    } else {
      restoreScene
    }
  }

  private def restoreNode {
    Platform.runLater(new Runnable {
        def run {
          list.add(node)
          stage.show
        }
      })
  }

  /**
   * This function restores the main Scene to the original Stage
   * from where the event has been triggered
   */
  private def restoreScene {
    Platform.runLater(new Runnable {
        def run {
          stage.setScene(scene)
          stage.show
        }
      })
  }

  /**
   * This function is used to get the BufferedImage of the
   * Container as JFXPanel
   * @param container
   * @param bounds
   * @return
   */
  private def toBufferedImage(container: Container, bounds: Bounds): BufferedImage = {
    val bufferedImage = new BufferedImage(bounds.getWidth.toInt,
                                          bounds.getHeight.toInt,
                                          BufferedImage.TYPE_INT_ARGB)

    val graphics = bufferedImage.getGraphics
    graphics.translate(-bounds.getMinX.toInt, -bounds.getMinY.toInt) // translating to upper-left corner
    container.paint(graphics)
    graphics.dispose
    bufferedImage
  }

}
