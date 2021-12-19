package flowdesigner.flowdesigner.business.util.raw.kit;

import io.vavr.Function2;
import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Pair;
import lombok.Getter;

/**
 * 用于实现拖放功能。将拖与放分离为两个方法，增加灵活性。应用程序需要拖放的条目众多，故将该类设计为静态类。
 */
public class DragListener {
    // 控件拖动后将要放置到哪个控件上
    @Getter
    private static Pane m_dropTarget = null;
    // 鼠标x坐标点相对于源控件的偏移值
    private static double offsetX;
    // 鼠标y坐标点相对于源控件的偏移值
    private static double offsetY;
    // 鼠标释放点的坐标值
    private static Point2D m_dropPoint = null;
    /**
     * 功能：在特定对象drag上启用拖放功能，启用后才能进行拖动。
     * @param drag  拖动对象
     * @param funCallback 拖放过程中的函数回调。可以处理感兴趣的Event消息事件.
     */
    public static void enableDrag(Node drag, Function2<Event, Object, String> funCallback) {
        // 开始拖动源控件
        drag.setOnDragDetected(mouseEvent -> {
            Dragboard dragboard = drag.startDragAndDrop(TransferMode.COPY_OR_MOVE);
            // 在setDragView中设置位置来进行鼠标的位置修正。
            dragboard.setDragView(drag.snapshot(null,null),mouseEvent.getX(),mouseEvent.getY());   //  @2
            ClipboardContent content = new ClipboardContent();
            String copy = drag.toString();
            if (funCallback != null) {
                // 在需要覆盖@2语句的场合重新设置拖放时显示的缩略图
                copy = funCallback.apply(mouseEvent, dragboard);
            }
            content.putString(copy);
            dragboard.setContent(content);

            drag.toFront();
            //计算偏移。通过getX获取的数据是相对于源控件的偏移，所以这个值可用于drop时的位置修正。
            offsetX = mouseEvent.getX();
            offsetY = mouseEvent.getY();

            mouseEvent.consume();
        });

        // 拖放完成。会发送DRAG_DONE事件到手势源来通知该手势是如何完成的。可通过调用事件的getTransferMode方法来获取传输模式。
        // 如果传输模式是NULL表示数据传输未发生。如果传输模式是MOVE则清空掉手势源上的数据
        drag.setOnDragDone(dragEvent -> {
            // 拖放手势完成后，计算控件目标坐标点（左上角坐标）
            var leftTopPoint = m_dropPoint;
            if (dragEvent.getTransferMode() == TransferMode.MOVE ) {
                // 由于move过程鼠标的位置不可能准确的在左上角，所以move完成需要对位置进行修正，
                leftTopPoint = new Point2D(m_dropPoint.getX()-offsetX,m_dropPoint.getY()-offsetY);
            } else if (dragEvent.getTransferMode() == TransferMode.COPY) {
                // copy不需要修正：放置的开始位置就是鼠标位置。
            }

            // 移动位置
            preSetPosition(drag,leftTopPoint);

            if (funCallback != null) {
                funCallback.apply(dragEvent, leftTopPoint);
            }
            dragEvent.consume();
        });
    }

    /**
     * 功能：实现放的功能。
     * @param funCallback
     * @param dropTarget  放置目标，可变对象.
     */
    public static void enableDrop(Node dropTarget, Function2<DragEvent, Node, String> funCallback) {
        dropTarget.setOnDragEntered(dragEvent -> {
            /* 拖放手势进入目标，提示客户它是一个真实的手势目标 */
            if (dragEvent.getGestureSource() != dropTarget && dragEvent.getDragboard().hasString()) {
                dropTarget.setStyle("-fx-border-color:Color\\.RED");
                if (funCallback != null) {
                    funCallback.apply(dragEvent, dropTarget);
                }
            }
            dragEvent.consume();
        });

        dropTarget.setOnDragExited(dragEvent -> {
            /* 拖放手势进入目标，提示客户它是一个真实的手势目标 */
            if (dragEvent.getGestureSource() != dropTarget && dragEvent.getDragboard().hasString()) {
                dropTarget.setStyle("-fx-border-color:Color\\.BLACK");
                if (funCallback != null) {
                    funCallback.apply(dragEvent, dropTarget);
                }
            }
            dragEvent.consume();
        });

        /**
         * 功能：设置传输模式。在 dropTarget.setOnDragDropped配合下可以将所设置的模式传递给源对象函数。
         * 模式设置：move - 如果drop和drag为父子关系。copy：其他关系。
         */
        dropTarget.setOnDragOver( dragEvent -> {
            if (dragEvent.getGestureSource() != dropTarget && dragEvent.getDragboard().hasString()) {
                if (((Node) dragEvent.getGestureSource()).getParent() == dropTarget) {
                    dragEvent.acceptTransferModes(TransferMode.MOVE);
                } else {
                    dragEvent.acceptTransferModes(TransferMode.COPY);
                }
            }
            dragEvent.consume();
        });

        /**
         * 数据被放下时的动作响应。
         * 其中dragEvent.setDropCompleted(success)函数决定drag.setOnDragDone接收到的传输模式。
         * 1、success=FALSE，transfer为null；
         * 2、success=TRUE，由函数dropTarget.setOnDragOver中dragEvent.acceptTransferModes决定；
         * 3、不在drop的范围内放下时，transfer为null；
         */
        dropTarget.setOnDragDropped(dragEvent -> {
            /* 数据被放下 */
            boolean success = false;
            Dragboard dragboard = dragEvent.getDragboard();
            if (dragboard.hasString()) {
                success = true;
                m_dropTarget = (Pane) dragEvent.getGestureTarget();
            }
            m_dropPoint = new Point2D(dragEvent.getX(),dragEvent.getY());
            dragEvent.setDropCompleted(success);
            dragEvent.consume();
        });

    }

    // 预设拖动后的控件位置，客户端需要修改位置的可以通过回调函数
    public static void preSetPosition(Node drag, Point2D leftTopPoint) {
        Parent parent = drag.getParent();
        if (parent.getClass().isAssignableFrom(AnchorPane.class)) {
            AnchorPane.setLeftAnchor(drag, leftTopPoint.getX());
            AnchorPane.setTopAnchor(drag, leftTopPoint.getY());
        }
    }

}
