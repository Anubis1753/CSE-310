"""A simple Tkinter program that accepts input and displays the results."""

import tkinter as tk
from tkinter import messagebox, ttk


def show_results() -> None:
    """Validate the form and display a personalized message."""
    name = name_entry.get().strip()
    age_text = age_entry.get().strip()
    favorite_number_text = favorite_number_entry.get().strip()

    if not name or not age_text or not favorite_number_text:
        messagebox.showerror("Missing information", "Please complete all fields.")
        return

    try:
        age = int(age_text)
        favorite_number = int(favorite_number_text)
    except ValueError:
        messagebox.showerror(
            "Invalid input", "Age and favorite number must be whole numbers."
        )
        return

    if age < 0:
        messagebox.showerror("Invalid age", "Age cannot be negative.")
        return

    number_type = "even" if favorite_number % 2 == 0 else "odd"
    result_label.config(
        text=(
            f"Hello, {name}!\n"
            f"You are {age} years old.\n"
            f"Your favorite number {favorite_number} is {number_type}.\n\n"
            "Thanks for using the program!"
        )
    )


def clear_form() -> None:
    """Clear all inputs and the displayed result."""
    name_entry.delete(0, tk.END)
    age_entry.delete(0, tk.END)
    favorite_number_entry.delete(0, tk.END)
    result_label.config(text="")
    name_entry.focus()


window = tk.Tk()
window.title("Favorite Number Checker")
window.geometry("420x360")
window.resizable(False, False)

main_frame = ttk.Frame(window, padding=20)
main_frame.pack(fill="both", expand=True)

ttk.Label(
    main_frame, text="Favorite Number Checker", font=("Arial", 18, "bold")
).pack(pady=(0, 5))
ttk.Label(main_frame, text="Enter your information below:").pack(pady=(0, 15))

form_frame = ttk.Frame(main_frame)
form_frame.pack()

ttk.Label(form_frame, text="Name:").grid(row=0, column=0, sticky="w", padx=5, pady=5)
name_entry = ttk.Entry(form_frame, width=30)
name_entry.grid(row=0, column=1, padx=5, pady=5)

ttk.Label(form_frame, text="Age:").grid(row=1, column=0, sticky="w", padx=5, pady=5)
age_entry = ttk.Entry(form_frame, width=30)
age_entry.grid(row=1, column=1, padx=5, pady=5)

ttk.Label(form_frame, text="Favorite number:").grid(
    row=2, column=0, sticky="w", padx=5, pady=5
)
favorite_number_entry = ttk.Entry(form_frame, width=30)
favorite_number_entry.grid(row=2, column=1, padx=5, pady=5)

button_frame = ttk.Frame(main_frame)
button_frame.pack(pady=15)
ttk.Button(button_frame, text="Show Results", command=show_results).grid(
    row=0, column=0, padx=5
)
ttk.Button(button_frame, text="Clear", command=clear_form).grid(row=0, column=1, padx=5)

result_label = ttk.Label(main_frame, text="", justify="center", anchor="center")
result_label.pack(pady=10)

name_entry.focus()
window.mainloop()
