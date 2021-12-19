# DragAndDrop
基于JavaFX的拖放工具类，将拖放操作高度集成，只需要一行代码即可实现拖或放的功能，适用于基于node的所有控件，并提供回调功能，可定制开发具体实现。

具体使用：
拖的功能
1、简单拖动功能
  一行代码即可：DragListener.enableDrag(form,null);
2、功能定制（可在回调中进行除拖动功能外进行其他代码编写）
        DragListener.enableDrag(tv_databaseTable, (event, db) -> {
            if(event.getEventType() == MouseEvent.DRAG_DETECTED) {
                TreeItem<String> selectedItem = tv_databaseTable.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    ((Dragboard)db).setDragView(selectedItem.getGraphic().snapshot(null,null),0,0);
                    return selectedItem.getValue();
                }
            } else if(event.getEventType() == DragEvent.DRAG_DONE) {
                TreeItem<String> selectedItem = tv_databaseTable.getSelectionModel().getSelectedItem();
                Pane formForTest = DynamicForm.createFromItem(selectedItem);
                DragListener.getM_dropTarget().getChildren().add(formForTest);
                Point2D pair1 = (Point2D) db;
                AnchorPane.setLeftAnchor(formForTest, pair1.getX());
                AnchorPane.setTopAnchor(formForTest, pair1.getY());
            }
            return "";
        });

 放的功能
 3、简单放的功能
  DragListener.enableDrop(anchorPane_schema_canvas001, null);
 4、放的功能回调（类似2）
