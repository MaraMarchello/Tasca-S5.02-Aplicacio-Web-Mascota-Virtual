import React, { useRef, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import { useTheme } from '../../contexts/ThemeContext';

interface CodeEditorProps {
  value: string;
  onChange: (value: string | undefined) => void;
  language?: string;
  height?: string;
  readOnly?: boolean;
  placeholder?: string;
  className?: string;
}

const CodeEditor: React.FC<CodeEditorProps> = ({
  value,
  onChange,
  language = 'java',
  height = '400px',
  readOnly = false,
  placeholder = '// Start coding here...',
  className = ''
}) => {
  const { theme } = useTheme();
  const editorRef = useRef<any>(null);

  const handleEditorDidMount = (editor: any, monaco: any) => {
    editorRef.current = editor;
    
    // Configure editor options
    editor.updateOptions({
      fontSize: 14,
      fontFamily: 'JetBrains Mono, Consolas, Monaco, monospace',
      lineHeight: 1.6,
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
      automaticLayout: true,
      wordWrap: 'on',
      tabSize: 2,
      insertSpaces: true,
      renderLineHighlight: 'line',
      selectionHighlight: false,
      occurrencesHighlight: false,
      renderValidationDecorations: 'on',
      scrollbar: {
        vertical: 'visible',
        horizontal: 'visible',
        useShadows: false,
        verticalHasArrows: false,
        horizontalHasArrows: false,
        verticalScrollbarSize: 8,
        horizontalScrollbarSize: 8
      }
    });

    // Add custom Java snippets
    if (language === 'java') {
      monaco.languages.registerCompletionItemProvider('java', {
        provideCompletionItems: (_model: any, _position: any) => {
          const suggestions = [
            {
              label: 'psvm',
              kind: monaco.languages.CompletionItemKind.Snippet,
              insertText: 'public static void main(String[] args) {\n\t$0\n}',
              insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
              documentation: 'Main method'
            },
            {
              label: 'sout',
              kind: monaco.languages.CompletionItemKind.Snippet,
              insertText: 'System.out.println($0);',
              insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
              documentation: 'Print line'
            },
            {
              label: 'class',
              kind: monaco.languages.CompletionItemKind.Snippet,
              insertText: 'public class ${1:ClassName} {\n\t$0\n}',
              insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
              documentation: 'Public class'
            }
          ];
          return { suggestions };
        }
      });
    }

    // Show placeholder when empty
    if (!value && placeholder) {
      editor.setValue(placeholder);
      editor.setSelection(new monaco.Selection(1, 1, 1, 1));
    }
  };

  const handleEditorChange = (value: string | undefined) => {
    if (value === placeholder) {
      onChange('');
    } else {
      onChange(value);
    }
  };

  useEffect(() => {
    if (editorRef.current && !value && placeholder) {
      editorRef.current.setValue(placeholder);
    }
  }, [value, placeholder]);

  return (
    <div className={`border border-border-light dark:border-border-dark rounded-lg overflow-hidden ${className}`}>
      <Editor
        height={height}
        language={language}
        value={value || placeholder}
        onChange={handleEditorChange}
        onMount={handleEditorDidMount}
        theme={theme === 'dark' ? 'vs-dark' : 'vs-light'}
        options={{
          readOnly,
          selectOnLineNumbers: true,
          roundedSelection: false,
          cursorStyle: 'line',
          automaticLayout: true,
          glyphMargin: false,
          folding: true,
          lineNumbers: 'on',
          lineDecorationsWidth: 0,
          lineNumbersMinChars: 3,
          contextmenu: true,
          mouseWheelZoom: true,
          smoothScrolling: true,
          cursorBlinking: 'blink'
        }}
        loading={
          <div className="flex items-center justify-center h-full bg-surface-light dark:bg-surface-dark">
            <div className="text-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500 mx-auto"></div>
              <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">Loading editor...</p>
            </div>
          </div>
        }
      />
    </div>
  );
};

export default CodeEditor; 