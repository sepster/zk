/* Binding.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Thu Feb  1 17:13:40     2007, Created by Henri
}}IS_NOTE

Copyright (C) 2006 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package org.zkoss.zkplus.databind;

import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.Express;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ComponentsCtrl;

import org.zkoss.util.ModificationException;
import org.zkoss.lang.Classes;
import org.zkoss.lang.Objects;
import org.zkoss.lang.reflect.Fields;

import java.io.Serializable;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ArrayList;

/**
 * A Data Binding that associate component+attr to an bean expression.
 *
 * @author Henri
 */
public class Binding {
	private DataBinder _binder;
	private Component _comp;
	private String _attr;
	private String _expression; //the bean expression
	private LinkedHashSet _loadWhenEvents;
	private String _saveWhenEvent;
	private boolean _loadable = true;
	private boolean _savable;
	private TypeConverter _converter;
	private String[] _paths; //bean reference path (a.b.c)
	
	/** Constrcutor to form a binding between UI component and backend data bean.
	 * @param binder the associated Data Binder.
	 * @param comp The concerned component
	 * @param attr The component attribute
	 * @param expr The bean expression.
	 * @param loadWhenEvents The event set when to load data.
	 * @param saveWhenEvent The event when to save data.
	 * @param access In the view of UI component: "load" load only, "both" load/save, 
	 *	"save" save only when doing data binding. null means using the default access 
	 *	natural of the component. e.g. Label.expr is "load", but Textbox.expr is "both".
	 * @param converter The converter class used to convert classes between component attribute 
	 * and the associated bean expression. null means using the default class conversion method.
	 */
	/*package*/ Binding(DataBinder binder, Component comp, String attr, String expr, 
		LinkedHashSet loadWhenEvents, String saveWhenEvent, String access, String converter) {
		_binder = binder;
		_comp = comp;
		setAttr(attr);
		setExpression(expr);
		setLoadWhenEvents(loadWhenEvents);
		setSaveWhenEvent(saveWhenEvent);
		setAccess(access);
		setConverter(converter);
		
	}

	/** Gets the associated Data Binder of this Binding.
	 */
	public DataBinder getBinder() {
		return _binder;
	}
	
	/** Gets the associated Component of this Binding.
	 */
	public Component getComponent() {
		return _comp;
	}
	
	/** Set component attribute name.
	 * @param attr component attribute.
	 */	
	/*package*/void setAttr(String attr) {
		_attr = attr;
	}

	/** Get component attribute name.
	 */
	public String getAttr() {
		return _attr;
	}
	
	/** Set bean expression (a.b.c).
	 */
	/*package*/ void setExpression(String expr) {
		_expression = expr;
		_paths = parseBeanExpression(expr);
	}

	//:TODO: handle function parsing	
	private String[] parseBeanExpression(String expr) {
		String[] paths = new String[1];
		paths[0] = expr;
		return paths;
	}

	/** Get bean expression, e.g. a.b.c.
	 */
	public String getExpression() {
		return _expression;
	}
	
	/**
	 * Internal methods. DO NOT USE THIS. 
	 * Get bean reference paths.
	 */
	/*package*/ String[] getPaths() {
		return _paths;
	}
	
	/** Set save-when event expression.
	 * @param saveWhenEvent the save-when expression.
	 */
	/*package*/ void setSaveWhenEvent(String saveWhenEvent) {
		if (saveWhenEvent != null) {
			_saveWhenEvent = saveWhenEvent;
		}
	}
	
	/** Get save-when event expression.
	 */
	public String getSaveWhenEvent() {
		return _saveWhenEvent;
	}
	
	/** Add load-when event expression.
	 * @param loadWhenEvent the load-when expression.
	 */
	/*package*/ void setLoadWhenEvents(LinkedHashSet loadWhenEvents) {
		_loadWhenEvents = loadWhenEvents;
	}
	
	/** Get load-when event expression set.
	 */
	public LinkedHashSet getLoadWhenEvents() {
		return _loadWhenEvents;
	}

	/** Set accessibility.
	 */
	/*package*/ void setAccess(String access) {
		if (access == null) { //default access to none
			return;
		}
		
		if ("both".equals(access)) {
			_loadable = true;
			_savable = true;
		} else if ("load".equals(access)) {
			_loadable = true;
			_savable = false;
		} else if ("save".equals(access)) {
			_loadable = false;
			_savable = true;
		} else if ("none".equals(access)) { //unknow access mode
			_loadable = false;
			_savable = false;
		} else {
			throw new UiException("Unknown DataBinder access mode. Should be \"both\", \"load\", \"save\", or \"none\": "+access);
		}
	}
	
	/** Whether the binding is loadable.
	 */
	public boolean isLoadable() {
		return _loadable;
	}
	
	/** Whether the binding is savable.
	 */
	public boolean isSavable() {
		return _savable;
	}

	/** Set the {@link TypeConverter}.
	 * @param cvtClsName the converter class name.
	 */
	/*package*/ void setConverter(String cvtClsName) {
		if (cvtClsName != null) {
			try {
				_converter = (TypeConverter) Classes.newInstanceByThread(cvtClsName);
			} catch (Exception ex) {
				throw UiException.Aide.wrap(ex);
			}
		}
	}
	
	/** Get the {@link TypeConverter}.
	 */
	public TypeConverter getConverter() {
		return _converter;
	}
	
	/** load bean value into the attribute of the specified component.
	 * @param comp the component.
	 */
	public void loadAttribute(Component comp) {
		if (!isLoadable() || _attr.startsWith("_") || _binder.isTemplate(comp) || comp.getPage() == null) { 
			return; //cannot load, a control attribute, or a detached component, skip!
		}
		Object bean = _binder.getBeanAndRegisterBeanSameNodes(comp, _expression);
		myLoadAttribute(comp, bean);
	}

	/** load bean value into the attribute of the specified component.
	 * @param comp the component.
	 * @param bean the bean value.
	 */
	public void loadAttribute(Component comp, Object bean) {
		if (!isLoadable() || _attr.startsWith("_") || _binder.isTemplate(comp) || comp.getPage() == null) { 
			return; //cannot load, a control attribute, or a detached component, skip!
		}
		myLoadAttribute(comp, bean);
	}
	
	private void myLoadAttribute(Component comp, Object bean) {
		try {
			if (_converter != null) {
				bean = _converter.coerceToUi(bean, comp);
			}
			Fields.set(comp, _attr, bean, _converter == null);
		} catch (ClassCastException ex) {
			throw UiException.Aide.wrap(ex);
		} catch (NoSuchMethodException ex) {
			throw UiException.Aide.wrap(ex);
		} catch (ModificationException ex) {
			throw UiException.Aide.wrap(ex);
		} catch (WrongValueException ex) {
			//Bug #1615371, try to use setRawValue()
			if ("value".equals(_attr)) {
				try {
					Fields.set(comp, "rawValue", bean, _converter == null);
				} catch (Exception ex1) {
					//exception
					throw ex;
				}
			} else {
				throw ex;
			}
		}
	}

	/** save into bean value from the attribute of the specified component.
	 * @param comp the component.
	 */
	public void saveAttribute(Component comp) {
		final Object[] vals = getAttributeValues(comp);
		saveAttributeValue(comp, vals);
	}
	
	private void saveAttributeValue(Component comp, Object[] vals) {
		if (vals == null) return;
		
		final Object val = vals[0];
		final Object rawval = vals[1];
		_binder.setBeanAndRegisterBeanSameNodes(comp, val, this, _expression, _converter == null, rawval);
	}		
	
	/** Get converted value and original value of this Binding.
	 */
	private Object[] getAttributeValues(Component comp) {
		if (!isSavable() || _attr.startsWith("_") || _binder.isTemplate(comp) || comp.getPage() == null) { 
			return null; //cannot save, a control attribute, or a detached component, skip!
		}
		try {
			final Object rawval = Fields.get(comp, _attr);
			final Object val = (_converter == null) ? rawval : _converter.coerceToBean(rawval, comp);
			return new Object[] {val, rawval};
		} catch (ClassCastException ex) {
			throw UiException.Aide.wrap(ex);
		} catch (NoSuchMethodException ex) {
			throw UiException.Aide.wrap(ex);
		}
	}		
	
	/*package*/ void registerSaveEvent(Component comp) {
		if (_saveWhenEvent != null) {
			final Object[] objs = ComponentsCtrl.parseEventExpression(comp, _saveWhenEvent);
			//objs[0] component, objs[1] event name
			final Component target = (Component) objs[0];
			final String evtname = (String) objs[1];

			SaveEventListener listener = (SaveEventListener)
				target.getAttribute("zk.SaveEventListener."+evtname);
			if (listener == null) {
				listener = new SaveEventListener();
				target.setAttribute("zk.SaveEventListener."+evtname, listener);
				target.addEventListener(evtname, listener);
			}
			listener.addDataTarget(this, comp);
		}
	}
	
	/*package*/ void registerLoadEvents(Component comp) {
		if (_loadWhenEvents != null) {
			for(final Iterator it = _loadWhenEvents.iterator(); it.hasNext(); ) {
				final String expr = (String) it.next();
				final Object[] objs = ComponentsCtrl.parseEventExpression(comp, expr);
				//objs[0] component, objs[1] event name
				final Component target = (Component) objs[0];
				final String evtname = (String) objs[1];
				
				LoadEventListener listener = (LoadEventListener)
					target.getAttribute("zk.LoadEventListener."+evtname);
				if (listener == null) {
					listener = new LoadEventListener();
					target.setAttribute("zk.LoadEventListener."+evtname, listener);
					target.addEventListener(evtname, listener);
				}
				listener.addDataTarget(this, comp);
			}
		}
	}
					
	//-- Object --//
	public String toString() {
		return "[binder:"+_binder+", comp:"+_comp+", attr:"+_attr+", expr:"+_expression
			+", load-when:"+_loadWhenEvents+", save-when:"+_saveWhenEvent
			+", load:"+_loadable+", save:"+_savable+", converter:"+_converter+"]";
	}
	
	private static class BindingInfo implements Serializable {
		private Binding _binding;
		private Component _comp;
		private Object[] _vals;
		
		public BindingInfo(Binding binding, Component comp, Object[] vals) {
			_binding = binding;
			_comp = comp;
			_vals = vals;
		}
		
		public Component getComponent() {
			return _comp;
		}
		
		public Binding getBinding() {
			return _binding;
		}
		
		public Object[] getAttributeValues() {
			return _vals;
		}
	}
	
	private static abstract class BaseEventListener implements EventListener, Express {
		protected List _dataTargets;
		
		public BaseEventListener() {
			_dataTargets = new ArrayList(8);
		}
		
		public void addDataTarget(Binding binding, Component comp) {
			_dataTargets.add(new BindingInfo(binding, comp, null));
		}
	}
			
	private static class LoadEventListener extends BaseEventListener {
		public LoadEventListener() {
			super();
		}
		public void onEvent(Event event) {
			for(final Iterator it = _dataTargets.iterator();it.hasNext();) {
				final BindingInfo bi = (BindingInfo) it.next();
				final Component dt = bi.getComponent();
				final Binding binding = bi.getBinding();
				final DataBinder binder = binding.getBinder();
				final Component dataTarget = binder.isTemplate(dt) ? 
					binder.lookupClone(event.getTarget(), dt) : dt;
				if (dataTarget != null) {
					binding.loadAttribute(dataTarget);
				}
			}
		}
	}

	private static class SaveEventListener extends BaseEventListener {
		public SaveEventListener() {
			super();
		}
		public void onEvent(Event event) {
			final Component target = event.getTarget();
			final List tmplist = new ArrayList(_dataTargets.size());
			
			//fire onSave for each binding
			for(final Iterator it = _dataTargets.iterator();it.hasNext();) {
				final BindingInfo bi = (BindingInfo) it.next();
				final Component dt = bi.getComponent();
				final Binding binding = bi.getBinding();
				final DataBinder binder = binding.getBinder();
				final Component dataTarget = binder.isTemplate(dt) ? 
					binder.lookupClone(event.getTarget(), dt) : dt;
				final Object[] vals = binding.getAttributeValues(dataTarget);
				if (dataTarget != null) {
					tmplist.add(new BindingInfo(binding, dataTarget, vals));
					Events.sendEvent(new BindingSaveEvent("onBindingSave", dataTarget, target, binding, vals[0]));
				}
			}
			
			//fire onValidate for target component
			Events.sendEvent(new Event("onBindingValidate", target));
			
			//saveAttribute for each binding
			for(final Iterator it = tmplist.iterator();it.hasNext();) {
				final BindingInfo bi = (BindingInfo) it.next();
				final Component dataTarget = bi.getComponent();
				final Binding binding = bi.getBinding();
				final Object[] vals = bi.getAttributeValues();
				final DataBinder binder = binding.getBinder();
				binding.saveAttributeValue(dataTarget, vals);
			}
		}
	}

}
