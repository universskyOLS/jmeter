/*
 * ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 * if any, must include the following acknowledgment:
 * "This product includes software developed by the
 * Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowledgment may appear in the software itself,
 * if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 * "Apache JMeter" must not be used to endorse or promote products
 * derived from this software without prior written permission. For
 * written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 * "Apache JMeter", nor may "Apache" appear in their name, without
 * prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 * 
 * @author <a href="mailto:jsalvata@apache.org">Jordi Salvat i Alabart</a>
 * @version $Id$
 */
package org.apache.jmeter.testbeans.gui;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyEditorSupport;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * This class implements a property editor for possibly null String properties
 * that supports custom editing (i.e.: provides a GUI component) based
 * on a combo box.
 * <p>
 * The provided GUI is a combo box with:
 * <ul>
 * <li>An option for "undefined" (corresponding to the null value), unless
 *		the <b>noUndefined</b> property is set.
 * <li>An option for each value in the <b>tags</b> property.
 * <li>The possibility to write your own value, unless the <b>noEdit</b>
 * 	 property is set.
 * </ul>
 */
class ComboStringEditor extends PropertyEditorSupport
	implements ItemListener
{
	protected static Logger log= LoggingManager.getLoggerForClass();

	/**
	 * The list of options to be offered by this editor.
	 */
	private String[] tags= new String[0];

	/**
	 * True iif the editor should not accept (nor produce) a null value.
	 */
	private boolean noUndefined= false;

	/**
	 * True iif the editor should not accept (nor produce) any non-null
	 * values different from the provided tags.
	 */
	private boolean noEdit= false;

	/**
	 * The edited property's default value.
	 */
	private String initialEditValue;

	private JComboBox combo;
	private DefaultComboBoxModel model; 
	private boolean startingEdit= false;
	    /* True iif we're currently processing an event triggered by the user
	     * selecting the "Edit" option. Used to prevent reverting the combo
	     * to non-editable during processing of secondary events.
	     */

	private static final Object UNDEFINED=
		new UniqueObject(JMeterUtils.getResString("property_undefined"));
	private static final Object EDIT=
		new UniqueObject(JMeterUtils.getResString("property_edit"));

	ComboStringEditor()
	{
		// Create the combo box we will use to edit this property:

		model= new DefaultComboBoxModel();
		model.addElement(UNDEFINED);
		model.addElement(EDIT);

		combo= new JComboBox(model);
		combo.addItemListener(this);
		combo.setEditable(false);
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyEditor#supportsCustomEditor()
	 */
	public boolean supportsCustomEditor()
	{
		return true;
	}

    /* (non-Javadoc)
     * @see java.beans.PropertyEditor#getCustomEditor()
     */
    public Component getCustomEditor()
    {
        return combo;
    }

	/* (non-Javadoc)
	 * @see java.beans.PropertyEditor#getValue()
	 */
	public Object getValue()
	{
		return getAsText();
	}
	
	/* (non-Javadoc)
	 * @see java.beans.PropertyEditor#getAsText()
	 */
	 public String getAsText()
	 {
		Object value= combo.getSelectedItem();

		if (value == UNDEFINED) return null;
		else return (String)value;
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyEditor#setValue()
	 */
	public void setValue(Object value)
	{
		setAsText((String)value);
	}
	
	/* (non-Javadoc)
	 * @see java.beans.PropertyEditor#setAsText()
	 */
    public void setAsText(String value)
	{
		combo.setEditable(true);

		if (value == null) combo.setSelectedItem(UNDEFINED);
		else combo.setSelectedItem(value);

		if (! startingEdit && combo.getSelectedIndex() >= 0)
		{
			combo.setEditable(false);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e)
	{
		if (e.getStateChange() == ItemEvent.SELECTED)
		{
			if (e.getItem() == EDIT) {
				startingEdit= true;
				startEditing();
				startingEdit= false;
			}
			else
			{
				if (!startingEdit && combo.getSelectedIndex() >= 0) 
				{
					combo.setEditable(false);
				}

				firePropertyChange();
			}
		}
	}

	private void startEditing()
	{
		JTextComponent textField=
			(JTextComponent)combo.getEditor().getEditorComponent();

		combo.setEditable(true);

		textField.requestFocus();

		String text= initialEditValue;
		if (initialEditValue == null) text= "";  // will revert to last valid value if invalid

		combo.setSelectedItem(text);

		int i= text.indexOf("${}");
		if (i != -1) textField.setCaretPosition(i+2);
		else textField.selectAll();
	}

	/**
	 * @return
	 */
	public String getInitialEditValue()
	{
		return initialEditValue;
	}

	/**
	 * @return
	 */
	public boolean getNoEdit()
	{
		return noEdit;
	}

	/**
	 * @return
	 */
	public boolean getNoUndefined()
	{
		return noUndefined;
	}

	/**
	 * @return
	 */
	public String[] getTags()
	{
		return tags;
	}

	/**
	 * @param object
	 */
	public void setInitialEditValue(String object)
	{
		initialEditValue= object;
	}

	/**
	 * @param b
	 */
	public void setNoEdit(boolean b)
	{
		if (noEdit == b) return;
		noEdit= b;

		if (noEdit) model.removeElement(EDIT);
		else model.addElement(EDIT);
	}

	/**
	 * @param b
	 */
	public void setNoUndefined(boolean b)
	{
		if (noUndefined == b) return;
		noUndefined= b;

		if (noUndefined) model.removeElement(UNDEFINED);
		else model.insertElementAt(UNDEFINED, 0);
	}

	/**
	 * @param strings
	 */
	public void setTags(String[] strings)
	{
		if (tags.equals(strings)) return;

		for (int i=0; i<tags.length; i++) model.removeElement(tags[i]);

		tags= strings==null ? new String[0] : strings;
		
		int b= noUndefined ? 0 : 1; // base index for tags
		for (int i=0; i<tags.length; i++) model.insertElementAt(tags[i], b+i);
	}

	/**
	 * This is a funny hack: if you use a plain String, 
	 * entering the text of the string in the editor will make the
	 * combo revert to that option -- which actually amounts to
	 * making that string 'reserved'. I preferred to avoid this by
	 * using a different type having a controlled .toString().
	 */
	private static class UniqueObject
	{
		private String s;
		
		UniqueObject(String s)
		{
			this.s= s;
		}
		
		public String toString()
		{
			return s;
		}
	}
	
	public static class Test extends junit.framework.TestCase
	{
		public Test(String name)
		{
			super(name);
		}

		private void testSetGet(ComboStringEditor e, Object value) throws Exception
		{
			e.setValue(value);
			assertEquals(value, e.getValue());
		}
		private void testSetGetAsText(ComboStringEditor e, String text) throws Exception
		{
			e.setAsText(text);
			assertEquals(text, e.getAsText());
		}
		public void testSetGet() throws Exception
		{
			ComboStringEditor e= new ComboStringEditor();
				
			testSetGet(e, "any string");
			testSetGet(e, "");
			testSetGet(e, null);
			testSetGet(e, "${var}");
		}
		public void testSetGetAsText() throws Exception
		{
			ComboStringEditor e= new ComboStringEditor();
				
			testSetGetAsText(e, "any string");
			testSetGetAsText(e, "");
			testSetGetAsText(e, null);
			testSetGetAsText(e, "${var}");

			// Check "Undefined" does not become a "reserved word":
			e.setAsText(UNDEFINED.toString());
			assertNotNull(e.getAsText());
		}
	}
}