import { useState, useEffect, useCallback, createContext, useContext } from 'react';

const ToastContext = createContext(null);

let toastId = 0;

export function useToast() {
  return useContext(ToastContext);
}

// ToastContainer — visual toasts, rendered inside Layout
export default function Toast() {
  const [toasts, setToasts] = useState([]);

  const addToast = useCallback((message, type = 'info', duration = 4000) => {
    const id = ++toastId;
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, duration);
  }, []);

  // Expose to window for non-React code
  useEffect(() => {
    window.__toast = addToast;
    return () => { window.__toast = null; };
  }, [addToast]);

  return (
    <ToastContext.Provider value={addToast}>
      <div className="toast-container">
        {toasts.map(t => (
          <div key={t.id} className={`toast ${t.type}`}>{t.message}</div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}