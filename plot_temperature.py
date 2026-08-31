#!/usr/bin/env python3
"""Plot temperatures recorded in temperature_data.txt."""

import argparse
import csv
import os
import sys
from datetime import datetime
from pathlib import Path

import matplotlib
import matplotlib.dates as mdates
import matplotlib.pyplot as plt


DATE_TIME_FORMAT = "%Y-%m-%d %H:%M:%S.%f"


def graphical_display_available():
    """Return whether Matplotlib can open a window in this environment."""
    if sys.platform.startswith("linux"):
        return bool(os.environ.get("DISPLAY") or os.environ.get("WAYLAND_DISPLAY"))

    non_interactive_backends = {"agg", "pdf", "pgf", "ps", "svg", "template"}
    return matplotlib.get_backend().lower() not in non_interactive_backends


def read_temperature_data(filename: Path):
    """Return the valid date/time and temperature values from a log file."""
    date_times = []
    temperatures = []

    with filename.open("r", encoding="utf-8", newline="") as data_file:
        rows = csv.DictReader(data_file, delimiter="\t")

        required_columns = {"date_time", "temperature_celsius"}
        if not rows.fieldnames or not required_columns.issubset(rows.fieldnames):
            raise ValueError(
                "Expected tab-separated columns: date_time and temperature_celsius"
            )

        for line_number, row in enumerate(rows, start=2):
            try:
                date_time = datetime.strptime(row["date_time"], DATE_TIME_FORMAT)
                temperature = float(row["temperature_celsius"])
            except (KeyError, TypeError, ValueError):
                print(f"Skipping invalid or incomplete row {line_number}")
                continue

            date_times.append(date_time)
            temperatures.append(temperature)

    if not temperatures:
        raise ValueError(f"No valid temperature readings found in {filename}")

    return date_times, temperatures


def plot_temperatures(date_times, temperatures, output: Path | None, show: bool):
    """Create the temperature-over-time plot."""
    figure, axes = plt.subplots(figsize=(10, 5))
    axes.plot(date_times, temperatures, color="tab:red", linewidth=2)
    axes.set_title("Temperature Over Time")
    axes.set_xlabel("Date and time")
    axes.set_ylabel("Temperature (°C)")
    axes.grid(True, alpha=0.3)
    axes.xaxis.set_major_formatter(mdates.DateFormatter("%H:%M:%S"))
    figure.autofmt_xdate()
    figure.tight_layout()

    if output is not None:
        figure.savefig(output, dpi=150)
        print(f"Plot saved to {output}")

    if show:
        plt.show()
    else:
        plt.close(figure)


def parse_arguments():
    script_directory = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(
        description="Plot temperature readings from a tab-separated text file."
    )
    parser.add_argument(
        "filename",
        nargs="?",
        type=Path,
        default=script_directory / "temperature_data.txt",
        help="temperature log to read (default: temperature_data.txt)",
    )
    parser.add_argument(
        "--output",
        type=Path,
        help="also save the plot to an image, for example temperature_plot.png",
    )
    parser.add_argument(
        "--no-show",
        action="store_true",
        help="do not open an interactive plot window",
    )
    return parser.parse_args()


def main():
    arguments = parse_arguments()
    output = arguments.output
    show = not arguments.no_show

    if show and not graphical_display_available():
        show = False
        print("No graphical display detected; saving the plot as an image instead.")

    if not show and output is None:
        output = Path(__file__).resolve().parent / "temperature_plot.png"

    try:
        date_times, temperatures = read_temperature_data(arguments.filename)
        plot_temperatures(
            date_times,
            temperatures,
            output,
            show=show,
        )
    except (OSError, ValueError) as error:
        raise SystemExit(f"Error: {error}") from error


if __name__ == "__main__":
    main()
