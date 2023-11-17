import sys

try:
    from PyQt6.QtWidgets import QApplication
except ImportError as e:
    print("PyQt6 not installed")
    sys.exit()

from widgets.main_window import MainWindow

text = sys.argv[1]

app = QApplication([])

window = MainWindow(text)
window.show()

app.exec()
