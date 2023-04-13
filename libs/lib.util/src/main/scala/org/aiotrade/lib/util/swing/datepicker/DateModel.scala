package org.aiotrade.lib.util.swing.datepicker

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.Calendar
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import scala.collection.mutable.HashSet
import scala.reflect.ClassTag

object DateModel {
  private val CalendarClass = classOf[java.util.Calendar]
  private val DateClass = classOf[java.util.Date]
  private val SqlDateClass = classOf[java.sql.Date]

  def apply[T](value: T): DateModel[T] = value match {
    case x: java.sql.Date => new DateModel[java.sql.Date](x).asInstanceOf[DateModel[T]]
    case x: java.util.Calendar => new DateModel[java.util.Calendar](x).asInstanceOf[DateModel[T]]
    case x: java.util.Date => new DateModel[java.util.Date](x).asInstanceOf[DateModel[T]]
  }

  def apply[T](clazz: Class[T]): DateModel[T] = clazz match {
    case SqlDateClass => new DateModel[java.sql.Date].asInstanceOf[DateModel[T]]
    case CalendarClass => new DateModel[java.util.Calendar].asInstanceOf[DateModel[T]]
    case DateClass => new DateModel[java.util.Date].asInstanceOf[DateModel[T]]
  }
}

class DateModel[T] protected(value$: T)(protected implicit val m: ClassTag[T]) {
  private var _value = Calendar.getInstance

  private val changeListeners = new HashSet[ChangeListener]
  private val propertyChangeListeners = new HashSet[PropertyChangeListener]

  value_=(value$)
  setToMidnight

  def this()(implicit m: ClassTag[T]) = this(null.asInstanceOf[T])

  def value: T = {
    m.toString match {
      case "java.util.Calendar" => _value.asInstanceOf[T]
      case "java.util.Date" => _value.getTime.clone.asInstanceOf[T]
      case "java.sql.Date" => new java.sql.Date(_value.getTimeInMillis).asInstanceOf[T]
      case _ => null.asInstanceOf[T]
    }
  }

  def value_=(value: T) {
    val cal = value match {
      case x: java.util.Calendar => x
      case x: java.util.Date =>
        val cal = Calendar.getInstance
        cal.setTime(x)
        cal
      case null => Calendar.getInstance
    }

    val oldYearValue = this._value.get(Calendar.YEAR)
    val oldMonthValue = this._value.get(Calendar.MONTH)
    val oldDayValue = this._value.get(Calendar.DATE)
    val oldValue = this._value
    this._value = cal
    setToMidnight
    fireChangeEvent
    firePropertyChange("year", oldYearValue, this._value.get(Calendar.YEAR))
    firePropertyChange("month", oldMonthValue, this._value.get(Calendar.MONTH))
    firePropertyChange("day", oldDayValue, this._value.get(Calendar.DATE))
    firePropertyChange("value", oldValue, value)
  }

  def day = _value.get(Calendar.DATE)

  def month = _value.get(Calendar.MONTH)

  def year = _value.get(Calendar.YEAR)

  def day_=(day: Int) {
    val oldDayValue = this._value.get(Calendar.DATE)
    val oldValue = value
    _value.set(Calendar.DATE, day)
    fireChangeEvent
    firePropertyChange("day", oldDayValue, this._value.get(Calendar.DATE));
    firePropertyChange("value", oldValue, value);
  }

  def addDay(add: Int) {
    val oldDayValue = this._value.get(Calendar.DATE)
    val oldValue = value
    _value.add(Calendar.DATE, add)
    fireChangeEvent
    firePropertyChange("day", oldDayValue, this._value.get(Calendar.DATE))
    firePropertyChange("value", oldValue, value)
  }

  def month_=(month: Int) {
    val oldMonthValue = this._value.get(Calendar.MONTH);
    val oldValue = value
    _value.set(Calendar.MONTH, month);
    fireChangeEvent
    firePropertyChange("month", oldMonthValue, this._value.get(Calendar.MONTH))
    firePropertyChange("value", oldValue, value)
  }

  def addMonth(add: Int) {
    val oldMonthValue = this._value.get(Calendar.MONTH)
    val oldValue = value
    _value.add(Calendar.MONTH, add)
    fireChangeEvent
    firePropertyChange("month", oldMonthValue, this._value.get(Calendar.MONTH))
    firePropertyChange("value", oldValue, value)
  }

  def year_=(year: Int) {
    val oldYearValue = this._value.get(Calendar.YEAR)
    val oldValue = value
    _value.set(Calendar.YEAR, year)
    fireChangeEvent
    firePropertyChange("year", oldYearValue, this._value.get(Calendar.YEAR))
    firePropertyChange("value", oldValue, value)
  }

  def addYear(add: Int) {
    val oldYearValue = this._value.get(Calendar.YEAR)
    val oldValue = value
    _value.add(Calendar.YEAR, add)
    fireChangeEvent
    firePropertyChange("year", oldYearValue, this._value.get(Calendar.YEAR))
    firePropertyChange("value", oldValue, value)
  }


  def setDate(year: Int, month: Int, day: Int) {
    val oldYearValue = this._value.get(Calendar.YEAR)
    val oldMonthValue = this._value.get(Calendar.MONTH)
    val oldDayValue = this._value.get(Calendar.DATE)
    val oldValue = value
    _value.set(year, month, day)
    fireChangeEvent
    firePropertyChange("year", oldYearValue, this._value.get(Calendar.YEAR))
    firePropertyChange("month", oldMonthValue, this._value.get(Calendar.MONTH))
    firePropertyChange("day", oldDayValue, this._value.get(Calendar.DATE))
    firePropertyChange("value", oldValue, value)
  }

  protected def setToMidnight {
    _value.set(Calendar.HOUR, 0)
    _value.set(Calendar.MINUTE, 0)
    _value.set(Calendar.SECOND, 0)
    _value.set(Calendar.MILLISECOND, 0)
  }

  def addChangeListener(changeListener: ChangeListener): Unit = synchronized {
    changeListeners.add(changeListener)
  }

  def removeChangeListener(changeListener: ChangeListener): Unit = synchronized {
    changeListeners.remove(changeListener)
  }

  protected def fireChangeEvent: Unit = synchronized {
    for (changeListener <- changeListeners) {
      changeListener.stateChanged(new ChangeEvent(this))
    }
  }

  def addPropertyChangeListener(listener: PropertyChangeListener): Unit = synchronized {
    propertyChangeListeners.add(listener)
  }

  def removePropertyChangeListener(listener: PropertyChangeListener): Unit = synchronized {
    propertyChangeListeners.remove(listener)
  }

  protected def firePropertyChange(propertyName: String, oldValue: Any, newValue: Any): Unit = synchronized {
    if (oldValue != null && newValue != null && oldValue == newValue) {
      return
    }

    for (listener <- propertyChangeListeners) {
      listener.propertyChange(new PropertyChangeEvent(this, propertyName, oldValue, newValue))
    }
  }
}
