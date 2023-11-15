import sys

from PyQt6.QtWidgets import QApplication
from widgets.main_window import MainWindow

text = sys.argv[1]

app = QApplication([])

window = MainWindow(text)
window.show()

app.exec()
