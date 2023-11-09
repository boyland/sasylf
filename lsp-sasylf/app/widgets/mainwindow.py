from PyQt6.QtWidgets import (
    QMainWindow,
    QCheckBox,
    QVBoxLayout,
    QLabel,
    QWidget,
    QScrollArea,
)


class MainWindow(QMainWindow):
    def __init__(self, text):
        super().__init__()

        central_widget = QWidget(self)
        self.setCentralWidget(central_widget)
        self.setWindowTitle("My App")

        layout = QVBoxLayout()

        self.scroll_area = QScrollArea(self)
        self.scroll_area.setWidget(QLabel(text))
        self.scroll_area.hide()

        self.button = QCheckBox("Show document text")
        self.button.clicked.connect(self.the_button_was_clicked)

        layout.addWidget(self.scroll_area)
        layout.addWidget(self.button)

        central_widget.setLayout(layout)

    def the_button_was_clicked(self):
        if self.button.isChecked():
            self.scroll_area.show()
        else:
            self.scroll_area.hide()
