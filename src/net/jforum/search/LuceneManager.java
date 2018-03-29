/*
 * Copyright (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following  disclaimer.
 * 2)  Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 * 
 * Created on 24/07/2007 12:23:05
 * 
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.search;

import java.io.IOException;

import net.jforum.entities.Post;
import net.jforum.exceptions.ForumException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;

/**
 * @author Rafael Steil
 * @version $Id: LuceneManager.java,v 1.16 2007/09/09 22:53:35 rafaelsteil Exp $
 */
public class LuceneManager implements SearchManager {
	
	private LuceneSearch search = null;
	private LuceneSettings settings = null;
	private LuceneIndexer indexer = null;
	
	/**
	 * @see net.jforum.search.SearchManager#init()
	 */
	public void init() {
		System.out.println("--> [LuceneManager.init] ......");
		try {
			System.out.println("DEBUG: start to instante LUCENE_ANALYZER for '" + SystemGlobals.getValue(ConfigKeys.LUCENE_ANALYZER) + "' ...");
			Analyzer analyzer = (Analyzer)Class.forName(SystemGlobals.getValue(ConfigKeys.LUCENE_ANALYZER)).newInstance();
			System.out.println("INFOR: start to instante the object of 'LuceneSettings' with 'analyzer'");
			settings = new LuceneSettings(analyzer);
			settings.useFSDirectory(SystemGlobals.getValue(ConfigKeys.LUCENE_INDEX_WRITE_PATH));
			removeLockFile();
			System.out.println("INFOR: start to instante the object of 'LuceneIndexer' with 'settings'");
			indexer = new LuceneIndexer(settings);
			System.out.println("INFOR: start to instante the object of 'LuceneContentCollector' with 'settings'");
			System.out.println("INFOR: start to instante the object of 'LuceneSearch' with 'contentCollector'");
			search = new LuceneSearch(settings, new LuceneContentCollector(settings));
			indexer.watchNewDocuDocumentAdded(search);
			System.out.println("DEBUG: indexer already be set as watching new document '" + SystemGlobals.getValue(ConfigKeys.LUCENE_ANALYZER) + "'");
			SystemGlobals.setObjectValue(ConfigKeys.LUCENE_SETTINGS, settings);
			System.out.println("INFOR: set the object of 'LuceneSettings' to 'SystemGlobals.globals.objectProperties'");
		} catch (Exception e) {
			throw new ForumException(e);
		}
	}
	
	public LuceneSearch luceneSearch() {
		return search;
	}
	
	public LuceneIndexer luceneIndexer() {
		return indexer;
	}
	
	public void removeLockFile() {
		System.out.println("--> [LuceneManager.removeLockFile] ......");
		try {
			if (IndexReader.isLocked(settings.directory())) {
				IndexReader.unlock(settings.directory());
				System.out.println("DEBUG: LuceneSettings.fs.directory (" + settings.directory() + ") is unlocked now ...");
			}
		} catch (IOException e) {
			throw new ForumException(e);
		}
	}
	
	/**
	 * @see net.jforum.search.SearchManager#create(net.jforum.entities.Post)
	 */
	public void create(Post post) {
		System.out.println("--> [LuceneManager.create] ......");
		indexer.create(post);
	}
	
	/**
	 * @see net.jforum.search.SearchManager#update(net.jforum.entities.Post)
	 */
	public void update(Post post) {
		System.out.println("--> [LuceneManager.update] ......");
		indexer.update(post);
	}

	/**
	 * @see net.jforum.search.SearchManager#search(net.jforum.search.SearchArgs)
	 */
	public SearchResult search(SearchArgs args) {
		System.out.println("--> [LuceneManager.search] ......");
		return search.search(args);
	}
	
	/**
	 * @see net.jforum.search.SearchManager#delete(net.jforum.entities.Post)
	 */
	public void delete(Post p) {
		System.out.println("--> [LuceneManager.delete] ......");
		indexer.delete(p);
	}
}
