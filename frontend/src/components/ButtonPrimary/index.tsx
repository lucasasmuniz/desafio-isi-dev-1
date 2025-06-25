import type { ReactNode } from 'react';
import './styles.css';

type Props = {
    text: ReactNode
}

export default function ButtonPrimary({text}: Props){
    return(
        <button className='button-primary'>{text}</button>
    );
}